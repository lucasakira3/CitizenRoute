import { useCallback, useEffect, useRef, useState } from "react";
import { prefereMenosMovimento } from "../utils/movimento.js";
import { desprojetar, projetar } from "../utils/projecao.js";

/**
 * Câmera do mapa 3D, feita sem biblioteca.
 *
 * As placas (HTML) ficam num plano que recebe, pelo CSS, o transform
 *   rotateX(inclinação) scale3d(zoom) rotateZ(giro) translate(-x, -y)
 * a partir de variáveis CSS (--cx, --cy, --zoom, --giro, --incl). O chão é um
 * canvas desenhado com a mesma conta (utils/projecao.js), no mesmo quadro: por
 * isso as placas nunca "escorregam" das ruas. Nada disso re-renderiza o React.
 *
 * Para arrastar e dar zoom no ponto certo, o hook faz a conta inversa da
 * perspectiva: descobre qual ponto do chão está embaixo do cursor.
 */

const RAD = Math.PI / 180;
const LIMITES = { zoomMin: 0.2, zoomMax: 18, inclMax: 62 };
const INCLINACAO_3D = 48;
const DISTANCIA_PARA_ARRASTE = 5;

function limitar(valor, minimo, maximo) {
  return Math.min(Math.max(valor, minimo), maximo);
}

function suavizarCurva(t) {
  return t < 0.5 ? 4 * t * t * t : 1 - (-2 * t + 2) ** 3 / 2;
}

/**
 * @param refVista elemento que recebe as variáveis CSS e os eventos
 * @param opcoes.interativo se false (prévia), só enquadra e não responde a arraste nem roda
 * @param opcoes.folga espaço ocupado por painéis por cima do mapa ({ esquerda, inferior } em px)
 * @param opcoes.area caixa {x0,y0,x1,y1} enquadrada em verCidade() (null enquanto o mapa carrega)
 * @param opcoes.plano tamanho do plano ({ largura, altura }), para a câmera não sair do mapa
 * @param opcoes.aoMudar chamada a cada quadro em que a câmera muda (redesenha o canvas)
 */
export function useCameraMapa(
  refVista,
  { refVariaveis = refVista, interativo = true, folga, area, plano, aoMudar, inclinacaoInicial = INCLINACAO_3D },
) {
  const camera = useRef({ x: 0, y: 0, zoom: 0.6, giro: 0, incl: 0 });
  const centro = useRef({ x: 0, y: 0, largura: 0, altura: 0 });
  const animacao = useRef(null);
  const quadro = useRef(null);
  const arrastou = useRef(false);
  const enquadrou = useRef(false);
  const [vista, setVista] = useState({ em3d: true });
  const vistaAtual = useRef(vista);
  const folgaAtual = useRef(folga);
  folgaAtual.current = folga;
  const planoAtual = useRef(plano);
  planoAtual.current = plano;
  const aoMudarAtual = useRef(aoMudar);
  aoMudarAtual.current = aoMudar;

  const limitarAoPlano = (c) => {
    const { largura = Infinity, altura = Infinity } = planoAtual.current ?? {};
    c.x = limitar(c.x, 0, largura);
    c.y = limitar(c.y, 0, altura);
  };

  /** Escreve a câmera nas variáveis CSS e redesenha o chão. Roda uma vez por quadro. */
  const escrever = useCallback(() => {
    quadro.current = null;
    const elemento = refVariaveis.current;
    if (!elemento) {
      return;
    }
    const c = camera.current;
    elemento.style.setProperty("--cx", c.x.toFixed(3));
    elemento.style.setProperty("--cy", c.y.toFixed(3));
    elemento.style.setProperty("--zoom", c.zoom.toFixed(4));
    elemento.style.setProperty("--giro", c.giro.toFixed(2));
    elemento.style.setProperty("--incl", c.incl.toFixed(2));
    // As placas ficam quase do mesmo tamanho na tela em qualquer zoom: crescem só um pouco ao aproximar
    const crescimento = limitar(0.95 + 0.12 * Math.log2(c.zoom), 0.75, 1.35);
    elemento.style.setProperty("--k", (crescimento / c.zoom).toFixed(5));
    // Só avisa o React quando muda algo que a interface mostra (o botão 2D/3D)
    const em3d = c.incl > 20;
    if (em3d !== vistaAtual.current.em3d) {
      vistaAtual.current = { em3d };
      setVista(vistaAtual.current);
    }
    aoMudarAtual.current?.(c, centro.current);
  }, [refVariaveis]);

  /** Pede um redesenho no próximo quadro (vários pedidos no mesmo quadro viram um só). */
  const aplicar = useCallback(() => {
    if (!quadro.current) {
      quadro.current = requestAnimationFrame(escrever);
    }
  }, [escrever]);

  useEffect(() => () => quadro.current && cancelAnimationFrame(quadro.current), []);

  /** Mede o elemento e calcula o centro da câmera, descontando os painéis por cima do mapa. */
  const medir = useCallback(() => {
    const elemento = refVista.current;
    if (!elemento) {
      return;
    }
    const { esquerda = 0, inferior = 0 } = folgaAtual.current ?? {};
    const largura = elemento.clientWidth;
    const altura = elemento.clientHeight;
    centro.current = {
      x: esquerda + (largura - esquerda) / 2,
      y: (altura - inferior) / 2,
      largura: largura - esquerda,
      altura: altura - inferior,
    };
    refVariaveis.current?.style.setProperty("--ox", `${centro.current.x}px`);
    refVariaveis.current?.style.setProperty("--oy", `${centro.current.y}px`);
  }, [refVista, refVariaveis]);

  const parar = useCallback(() => {
    if (animacao.current) {
      cancelAnimationFrame(animacao.current);
      animacao.current = null;
    }
  }, []);

  /** Move a câmera até o destino. Sem animação quando a pessoa pediu menos movimento. */
  const voarPara = useCallback(
    (destino, { duracao = 520 } = {}) => {
      parar();
      const inicio = { ...camera.current };
      const fim = { ...inicio, ...destino };
      fim.zoom = limitar(fim.zoom, LIMITES.zoomMin, LIMITES.zoomMax);
      fim.incl = limitar(fim.incl, 0, LIMITES.inclMax);
      // Gira pelo caminho mais curto
      let deltaGiro = (((fim.giro - inicio.giro) % 360) + 540) % 360 - 180;
      if (Math.abs(deltaGiro) < 0.01) {
        deltaGiro = 0;
      }
      if (prefereMenosMovimento() || duracao === 0) {
        camera.current = { ...fim, giro: inicio.giro + deltaGiro };
        aplicar();
        return;
      }
      const comeco = performance.now();
      const passo = (agora) => {
        const t = suavizarCurva(Math.min((agora - comeco) / duracao, 1));
        camera.current = {
          x: inicio.x + (fim.x - inicio.x) * t,
          y: inicio.y + (fim.y - inicio.y) * t,
          // Zoom em escala logarítmica: a aproximação parece constante
          zoom: Math.exp(Math.log(inicio.zoom) + (Math.log(fim.zoom) - Math.log(inicio.zoom)) * t),
          giro: inicio.giro + deltaGiro * t,
          incl: inicio.incl + (fim.incl - inicio.incl) * t,
        };
        escrever();
        animacao.current = t < 1 ? requestAnimationFrame(passo) : null;
      };
      animacao.current = requestAnimationFrame(passo);
    },
    [aplicar, escrever, parar],
  );

  /** Calcula a câmera que mostra uma área inteira, com o giro e a inclinação pedidos. */
  const enquadramento = useCallback(
    (caixa, { giro = camera.current.giro, incl = camera.current.incl, margem = 28, zoomMaximo = 2.2 } = {}) => {
      medir();
      const { largura, altura } = centro.current;
      const cantos = [
        [caixa.x0, caixa.y0],
        [caixa.x1, caixa.y0],
        [caixa.x0, caixa.y1],
        [caixa.x1, caixa.y1],
      ];
      const centralizar = (zoom) => {
        const tentativa = { x: (caixa.x0 + caixa.x1) / 2, y: (caixa.y0 + caixa.y1) / 2, zoom, giro, incl };
        let limites = null;
        // Duas passadas: a perspectiva deixa a parte de baixo maior, então o centro precisa subir um pouco
        for (let i = 0; i < 3; i += 1) {
          const tela = cantos.map(([u, v]) => projetar(tentativa, u, v));
          if (tela.some((p) => p.atras)) {
            return null;
          }
          limites = {
            x0: Math.min(...tela.map((p) => p.x)),
            x1: Math.max(...tela.map((p) => p.x)),
            y0: Math.min(...tela.map((p) => p.y)),
            y1: Math.max(...tela.map((p) => p.y)),
          };
          const meio = desprojetar(tentativa, (limites.x0 + limites.x1) / 2, (limites.y0 + limites.y1) / 2);
          tentativa.x = meio.x;
          tentativa.y = meio.y;
        }
        return { tentativa, limites };
      };
      let baixo = LIMITES.zoomMin;
      let alto = zoomMaximo;
      let melhor = centralizar(baixo)?.tentativa;
      for (let i = 0; i < 22; i += 1) {
        const meio = (baixo + alto) / 2;
        const resultado = centralizar(meio);
        const cabe =
          resultado &&
          resultado.limites.x1 - resultado.limites.x0 <= largura - margem * 2 &&
          resultado.limites.y1 - resultado.limites.y0 <= altura - margem * 2;
        if (cabe) {
          baixo = meio;
          melhor = resultado.tentativa;
        } else {
          alto = meio;
        }
      }
      return melhor;
    },
    [medir],
  );

  const verArea = useCallback(
    (caixa, opcoes = {}) => {
      const destino = enquadramento(caixa, opcoes);
      if (destino) {
        voarPara(destino, opcoes);
      }
    },
    [enquadramento, voarPara],
  );

  const verCidade = useCallback(
    (opcoes = {}) => area && verArea(area, { giro: 0, incl: vistaAtual.current.em3d ? INCLINACAO_3D : 0, ...opcoes }),
    [area, verArea],
  );

  /** Aproxima ou afasta mantendo parado o ponto do chão embaixo de (sx, sy). */
  const zoomNoPonto = useCallback(
    (fator, sx = 0, sy = 0) => {
      const c = camera.current;
      const antes = desprojetar(c, sx, sy);
      c.zoom = limitar(c.zoom * fator, LIMITES.zoomMin, LIMITES.zoomMax);
      const depois = desprojetar(c, sx, sy);
      c.x += antes.x - depois.x;
      c.y += antes.y - depois.y;
      limitarAoPlano(c);
      aplicar();
    },
    [aplicar],
  );

  const zoomBotao = useCallback(
    (fator) => voarPara({ zoom: camera.current.zoom * fator }, { duracao: 260 }),
    [voarPara],
  );

  const girar = useCallback((graus) => voarPara({ giro: camera.current.giro + graus }, { duracao: 300 }), [voarPara]);

  const apontarNorte = useCallback(() => voarPara({ giro: 0 }, { duracao: 360 }), [voarPara]);

  const alternar3d = useCallback(
    () => voarPara({ incl: vistaAtual.current.em3d ? 0 : INCLINACAO_3D }, { duracao: 420 }),
    [voarPara],
  );

  /** Leva a câmera até um ponto do chão, aproximando se estiver longe. */
  const focar = useCallback(
    (x, y, zoomMinimo = 5) =>
      voarPara({
        x,
        y,
        zoom: Math.max(camera.current.zoom, zoomMinimo),
        incl: vistaAtual.current.em3d ? Math.max(camera.current.incl, 40) : 0,
      }),
    [voarPara],
  );

  // Primeiro enquadramento, assim que o mapa carrega: começa visto de cima e "deita" a cidade.
  useEffect(() => {
    if (!area || enquadrou.current) {
      return undefined;
    }
    enquadrou.current = true;
    medir();
    const destino = enquadramento(area, { giro: 0, incl: inclinacaoInicial });
    if (!destino) {
      return undefined;
    }
    camera.current = { ...destino, incl: 0, zoom: destino.zoom * 0.88, giro: -14 };
    escrever();
    const espera = setTimeout(() => voarPara(destino, { duracao: 900 }), 120);
    return () => {
      clearTimeout(espera);
      parar();
    };
    // Só uma vez: depois disso quem move a câmera é a pessoa.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [area]);

  // Mudou o tamanho da janela (ou a folga dos painéis): recalcula o centro sem mexer no que se vê.
  useEffect(() => {
    const elemento = refVista.current;
    if (!elemento || typeof ResizeObserver === "undefined") {
      return undefined;
    }
    const observador = new ResizeObserver(() => {
      medir();
      aplicar();
      if (!interativo && area) {
        const destino = enquadramento(area, { giro: camera.current.giro, incl: camera.current.incl });
        if (destino && !animacao.current) {
          camera.current = destino;
          aplicar();
        }
      }
    });
    observador.observe(elemento);
    return () => observador.disconnect();
  }, [refVista, medir, enquadramento, aplicar, area, interativo]);

  useEffect(() => {
    medir();
    aplicar();
  }, [folga?.esquerda, folga?.inferior, medir, aplicar]);

  // ----------------------------------------------------- arrastar, pinça e roda
  useEffect(() => {
    const elemento = refVista.current;
    if (!elemento || !interativo) {
      return undefined;
    }
    const ponteiros = new Map();
    let modo = null;
    let inicio = null;

    const posicao = (evento) => {
      const retangulo = elemento.getBoundingClientRect();
      return {
        x: evento.clientX - retangulo.left - centro.current.x,
        y: evento.clientY - retangulo.top - centro.current.y,
      };
    };

    const aoApertar = (evento) => {
      if (evento.pointerType === "mouse" && evento.button !== 0 && evento.button !== 2) {
        return;
      }
      parar();
      ponteiros.set(evento.pointerId, posicao(evento));
      arrastou.current = false;
      inicio = { x: evento.clientX, y: evento.clientY };
      if (ponteiros.size === 2) {
        modo = "pinca";
      } else {
        const girando = evento.button === 2 || evento.ctrlKey || evento.shiftKey || evento.altKey;
        modo = girando ? "girar" : "mover";
      }
    };

    const aoMover = (evento) => {
      if (!ponteiros.has(evento.pointerId)) {
        return;
      }
      const anterior = ponteiros.get(evento.pointerId);
      const atual = posicao(evento);
      if (!arrastou.current) {
        if (Math.hypot(evento.clientX - inicio.x, evento.clientY - inicio.y) < DISTANCIA_PARA_ARRASTE) {
          return;
        }
        // Só captura o ponteiro depois que virou arraste: um clique simples continua chegando à placa.
        arrastou.current = true;
        elemento.setPointerCapture?.(evento.pointerId);
        elemento.classList.add("mapa3d--arrastando");
      }
      const c = camera.current;
      if (modo === "pinca" && ponteiros.size === 2) {
        const [idOutro] = [...ponteiros.keys()].filter((id) => id !== evento.pointerId);
        const outro = ponteiros.get(idOutro);
        const meioAntes = { x: (anterior.x + outro.x) / 2, y: (anterior.y + outro.y) / 2 };
        const meioDepois = { x: (atual.x + outro.x) / 2, y: (atual.y + outro.y) / 2 };
        const distanciaAntes = Math.hypot(anterior.x - outro.x, anterior.y - outro.y) || 1;
        const distanciaDepois = Math.hypot(atual.x - outro.x, atual.y - outro.y) || 1;
        const anguloAntes = Math.atan2(anterior.y - outro.y, anterior.x - outro.x);
        const anguloDepois = Math.atan2(atual.y - outro.y, atual.x - outro.x);
        const a = desprojetar(c, meioAntes.x, meioAntes.y);
        const b = desprojetar(c, meioDepois.x, meioDepois.y);
        c.x += a.x - b.x;
        c.y += a.y - b.y;
        c.giro += (anguloDepois - anguloAntes) / RAD;
        ponteiros.set(evento.pointerId, atual);
        zoomNoPonto(distanciaDepois / distanciaAntes, meioDepois.x, meioDepois.y);
        return;
      }
      if (modo === "girar") {
        c.giro += (atual.x - anterior.x) * 0.3;
        c.incl = limitar(c.incl - (atual.y - anterior.y) * 0.25, 0, LIMITES.inclMax);
      } else {
        const a = desprojetar(c, anterior.x, anterior.y);
        const b = desprojetar(c, atual.x, atual.y);
        c.x += a.x - b.x;
        c.y += a.y - b.y;
        limitarAoPlano(c);
      }
      ponteiros.set(evento.pointerId, atual);
      aplicar();
    };

    const aoSoltar = (evento) => {
      ponteiros.delete(evento.pointerId);
      if (ponteiros.size === 0) {
        modo = null;
        elemento.classList.remove("mapa3d--arrastando");
      } else if (ponteiros.size === 1) {
        modo = "mover";
      }
    };

    // Depois de um arraste, o clique que o navegador dispara ao soltar é ignorado.
    const aoClicar = (evento) => {
      if (arrastou.current) {
        evento.stopPropagation();
        evento.preventDefault();
        arrastou.current = false;
      }
    };

    const aoRolar = (evento) => {
      evento.preventDefault();
      parar();
      const { x, y } = posicao(evento);
      const intensidade = evento.deltaMode === 1 ? 0.05 : 0.0016;
      zoomNoPonto(Math.exp(-evento.deltaY * intensidade), x, y);
    };

    const semMenu = (evento) => evento.preventDefault();

    elemento.addEventListener("pointerdown", aoApertar);
    elemento.addEventListener("pointermove", aoMover);
    elemento.addEventListener("pointerup", aoSoltar);
    elemento.addEventListener("pointercancel", aoSoltar);
    elemento.addEventListener("click", aoClicar, true);
    elemento.addEventListener("wheel", aoRolar, { passive: false });
    elemento.addEventListener("contextmenu", semMenu);
    return () => {
      elemento.removeEventListener("pointerdown", aoApertar);
      elemento.removeEventListener("pointermove", aoMover);
      elemento.removeEventListener("pointerup", aoSoltar);
      elemento.removeEventListener("pointercancel", aoSoltar);
      elemento.removeEventListener("click", aoClicar, true);
      elemento.removeEventListener("wheel", aoRolar);
      elemento.removeEventListener("contextmenu", semMenu);
    };
  }, [refVista, interativo, aplicar, parar, zoomNoPonto]);

  /** Teclado: setas movem, + e − aproximam, Q e E giram, T alterna 2D e 3D, Home volta à cidade toda. */
  const aoTeclar = useCallback(
    (evento) => {
      if (!interativo || evento.target !== evento.currentTarget) {
        return;
      }
      const c = camera.current;
      const passo = 70 / c.zoom;
      const g = c.giro * RAD;
      // As setas andam na direção da tela, não do norte
      const mover = (frente, lado) =>
        voarPara(
          {
            x: c.x + lado * Math.cos(g) * passo + frente * Math.sin(g) * passo,
            y: c.y - lado * Math.sin(g) * passo + frente * Math.cos(g) * passo,
          },
          { duracao: 180 },
        );
      const acoes = {
        ArrowUp: () => mover(-1, 0),
        ArrowDown: () => mover(1, 0),
        ArrowLeft: () => mover(0, -1),
        ArrowRight: () => mover(0, 1),
        "+": () => zoomBotao(1.4),
        "=": () => zoomBotao(1.4),
        "-": () => zoomBotao(1 / 1.4),
        q: () => girar(-20),
        e: () => girar(20),
        t: alternar3d,
        Home: () => verCidade(),
      };
      const acao = acoes[evento.key.length === 1 ? evento.key.toLowerCase() : evento.key];
      if (acao) {
        evento.preventDefault();
        acao();
      }
    },
    [interativo, voarPara, zoomBotao, girar, alternar3d, verCidade],
  );

  /** Ponto do chão embaixo de uma posição do mouse (clientX, clientY). */
  const pontoNaTela = useCallback(
    (clientX, clientY) => {
      const retangulo = refVista.current.getBoundingClientRect();
      return desprojetar(
        camera.current,
        clientX - retangulo.left - centro.current.x,
        clientY - retangulo.top - centro.current.y,
      );
    },
    [refVista],
  );

  return {
    vista,
    camera,
    centro,
    arrastou,
    aplicar,
    pontoNaTela,
    aoTeclar,
    voarPara,
    verArea,
    verCidade,
    focar,
    zoomBotao,
    girar,
    apontarNorte,
    alternar3d,
  };
}
