import { memo, useCallback, useEffect, useImperativeHandle, useMemo, useRef, useState } from "react";
import { useApp } from "../contexto.js";
import { useCameraMapa } from "../hooks/useCameraMapa.js";
import { desenharMapa } from "../utils/desenhoMapa.js";
import { formatarNumero } from "../utils/formato.js";
import {
  carregarMapa,
  comprimento,
  pontoNaFracao,
  tracadoDaVia,
  zonaDaRegiao,
  zonaNoPonto,
} from "../utils/mapaCidade.js";
import Icone from "./Icone.jsx";

export { zonaDaRegiao };

/**
 * Mapa de São Paulo com as ocorrências ativas.
 *
 * - O chão (ruas, rios, parques, prédios e nomes) vem do OpenStreetMap e é desenhado
 *   num canvas a cada quadro (utils/desenhoMapa.js).
 * - Cada ocorrência é uma placa de advertência 3D (HTML + CSS 3D) em cima da via
 *   atingida, na cor do risco para o modal escolhido. As vias atingidas acendem.
 * - Para um cidadão logado, as vias do trajeto dele aparecem em azul.
 *
 * modo "tela": página Mapa, câmera livre. modo "previa": quadro do Painel, câmera parada.
 */

const ORDEM_RISCO = { BAIXO: 1, MODERADO: 2, ALTO: 3, CRITICO: 4 };
const ORDEM_IMPACTO = { BLOQUEIO_TOTAL: 4, PARCIAL: 3, LENTIDAO: 2, SEM_INTERDICAO: 1 };
const NIVEIS = ["BAIXO", "MODERADO", "ALTO", "CRITICO"];

// Onde ficam as placas de vias que não foram achadas no mapa, em volta do centro da zona
const ANEL = [[0, 0], [40, 18], [-36, 22], [8, -40], [-32, -30], [52, -14], [-54, 0], [20, 46]];

/** Linhas da via que ficam dentro da cidade (o nome pode se repetir num município vizinho). */
function linhasNaCidade(dados, indices) {
  const dentro = indices.filter((indice) => {
    const { caixa } = dados.ruas[indice];
    return zonaNoPonto(dados, (caixa[0] + caixa[2]) / 2, (caixa[1] + caixa[3]) / 2);
  });
  return dentro.length ? dentro : indices;
}

function montarSinais(ocorrencias, dados) {
  const afetadas = new Map();
  const porVia = new Map();
  const semTracado = new Map();
  let foraDoMapa = 0;

  ocorrencias.forEach((ocorrencia) => {
    const comTracado = ocorrencia.vias
      .map((item) => ({ ...item, indices: linhasNaCidade(dados, tracadoDaVia(dados, item.via)) }))
      .filter((item) => item.indices.length);

    comTracado.forEach((item) => {
      const atual = afetadas.get(item.via.id);
      const nivel = ocorrencia.risco.nivel;
      if (!atual) {
        afetadas.set(item.via.id, { indices: item.indices, nivel, impacto: item.nivelImpacto, ids: [ocorrencia.id] });
      } else {
        atual.ids.push(ocorrencia.id);
        if (ORDEM_RISCO[nivel] > ORDEM_RISCO[atual.nivel]) atual.nivel = nivel;
        if (ORDEM_IMPACTO[item.nivelImpacto] > ORDEM_IMPACTO[atual.impacto]) atual.impacto = item.nivelImpacto;
      }
    });

    if (comTracado.length) {
      // A placa fica na via mais atingida (bloqueio total antes de lentidão)
      const principal = [...comTracado].sort(
        (a, b) => ORDEM_IMPACTO[b.nivelImpacto] - ORDEM_IMPACTO[a.nivelImpacto],
      )[0];
      const grupo = porVia.get(principal.via.id) ?? { indices: principal.indices, ocorrencias: [] };
      grupo.ocorrencias.push(ocorrencia);
      porVia.set(principal.via.id, grupo);
      return;
    }

    const zona = dados.zonas.find((z) => z.chave === zonaDaRegiao(ocorrencia.vias[0]?.via.regiao?.nome));
    if (!zona) {
      foraDoMapa += 1;
      return;
    }
    const grupo = semTracado.get(zona.chave) ?? { zona, ocorrencias: [] };
    grupo.ocorrencias.push(ocorrencia);
    semTracado.set(zona.chave, grupo);
  });

  const sinais = [];
  porVia.forEach(({ indices, ocorrencias: daVia }) => {
    // A linha mais comprida da via; várias ocorrências na mesma via ficam espalhadas por ela
    const linha = indices
      .map((indice) => dados.ruas[indice].pontos)
      .reduce((a, b) => (comprimento(b) > comprimento(a) ? b : a));
    daVia.forEach((ocorrencia, indice) => {
      const fracao = daVia.length === 1 ? 0.5 : 0.2 + (0.6 * (indice + 1)) / (daVia.length + 1);
      const { x, y } = pontoNaFracao(linha, fracao);
      sinais.push({ ocorrencia, x, y });
    });
  });
  semTracado.forEach(({ zona, ocorrencias: daZona }) => {
    daZona.forEach((ocorrencia, indice) => {
      const [dx, dy] = ANEL[indice % ANEL.length];
      sinais.push({ ocorrencia, x: zona.centro[0] + dx, y: zona.centro[1] + dy, semTracado: true });
    });
  });
  // Mais arriscadas primeiro: são as primeiras a se levantar na animação de entrada
  sinais.sort((a, b) => b.ocorrencia.risco.valor - a.ocorrencia.risco.valor);
  return { sinais, afetadas, foraDoMapa };
}

function piorRiscoPorZona(ocorrencias) {
  const resultado = {};
  ocorrencias.forEach((ocorrencia) => {
    new Set(ocorrencia.vias.map((item) => zonaDaRegiao(item.via.regiao?.nome))).forEach((chave) => {
      if (chave && (!resultado[chave] || ORDEM_RISCO[ocorrencia.risco.nivel] > ORDEM_RISCO[resultado[chave]])) {
        resultado[chave] = ocorrencia.risco.nivel;
      }
    });
  });
  return resultado;
}

export default function MapaCidade({
  ocorrencias,
  modo = "tela",
  zonaSelecionada = null,
  aoSelecionarZona,
  selecionada = null,
  aoSelecionar,
  trajeto = [],
  folga,
  ref,
}) {
  const { rotulo } = useApp();
  const refVista = useRef(null);
  const refMoldura = useRef(null);
  const refCanvas = useRef(null);
  const refDados = useRef(null);
  const refDestaque = useRef({});
  const refMemoria = useRef({});
  const tela = modo === "tela";
  const [dados, setDados] = useState(null);
  const [erroMapa, setErroMapa] = useState(null);
  const [tentativa, setTentativa] = useState(0);
  // Depois da entrada, as placas que trocam de cor (outro modal) se levantam sem esperar
  const [pronto, setPronto] = useState(false);
  refDados.current = dados;

  useEffect(() => {
    let ativo = true;
    setErroMapa(null);
    carregarMapa()
      .then((carregado) => ativo && setDados(carregado))
      .catch((erro) => ativo && setErroMapa(erro));
    return () => {
      ativo = false;
    };
  }, [tentativa]);

  useEffect(() => {
    if (!dados) return undefined;
    const espera = setTimeout(() => setPronto(true), 2200);
    return () => clearTimeout(espera);
  }, [dados]);

  const [tipoArea] = useState(() => {
    if (window.matchMedia("(max-width: 520px)").matches) return "celular";
    return !tela || window.matchMedia("(max-width: 900px)").matches ? "nucleo" : "cidade";
  });

  // Desenha o chão. A câmera chama esta função uma vez por quadro em que algo muda.
  const desenhar = useCallback((cameraAtual, centro) => {
    const canvas = refCanvas.current;
    const mapa = refDados.current;
    if (!canvas || !mapa) return;
    const largura = canvas.clientWidth;
    const altura = canvas.clientHeight;
    if (!largura || !altura) return;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    if (canvas.width !== Math.round(largura * dpr) || canvas.height !== Math.round(altura * dpr)) {
      canvas.width = Math.round(largura * dpr);
      canvas.height = Math.round(altura * dpr);
    }
    desenharMapa(canvas.getContext("2d"), {
      dados: mapa,
      camera: cameraAtual,
      largura,
      altura,
      ox: centro.x,
      oy: centro.y,
      dpr,
      destaque: refDestaque.current,
      memoria: refMemoria.current,
    });
  }, []);

  const camera = useCameraMapa(refVista, {
    refVariaveis: refMoldura,
    interativo: tela,
    folga,
    area: dados?.areas[tipoArea] ?? null,
    plano: dados,
    aoMudar: desenhar,
  });
  const { vista, verArea, verCidade, focar, aplicar } = camera;

  const { sinais, afetadas, foraDoMapa } = useMemo(
    () => (dados ? montarSinais(ocorrencias, dados) : { sinais: [], afetadas: new Map(), foraDoMapa: 0 }),
    [ocorrencias, dados],
  );
  const indicesTrajeto = useMemo(
    () => (dados ? trajeto.flatMap((vm) => linhasNaCidade(dados, tracadoDaVia(dados, vm.via))) : []),
    [trajeto, dados],
  );
  const riscoZona = useMemo(() => piorRiscoPorZona(ocorrencias), [ocorrencias]);

  // O que o desenho destaca: vias com ocorrência, trajeto e zona escolhida
  useEffect(() => {
    refDestaque.current = {
      afetadas: [...afetadas.values()].map((item) => ({
        indices: item.indices,
        nivel: item.nivel,
        impacto: item.impacto,
        forte: Boolean(selecionada) && item.ids.includes(selecionada),
        fraca: Boolean(selecionada) && !item.ids.includes(selecionada),
      })),
      trajeto: indicesTrajeto,
      zonaSelecionada,
      riscoZona,
      obstaculos: sinais.map((sinal) => [sinal.x, sinal.y]),
    };
    aplicar();
  }, [afetadas, sinais, indicesTrajeto, zonaSelecionada, riscoZona, selecionada, aplicar]);

  const verZona = useCallback(
    (chave) => {
      const zona = dados?.zonas.find((item) => item.chave === chave);
      if (zona) verArea(zona.caixa, { zoomMaximo: 3 });
    },
    [dados, verArea],
  );

  // A página usa estas funções para mover a câmera (clique na lista, filtro de zona)
  useImperativeHandle(
    ref,
    () => ({
      focarOcorrencia(id) {
        const sinal = sinais.find((item) => item.ocorrencia.id === id);
        if (sinal) focar(sinal.x, sinal.y, 6);
      },
      verZona,
      verCidade: () => verCidade(),
    }),
    [sinais, focar, verZona, verCidade],
  );

  const escolherSinal = useCallback(
    (sinal) => {
      aoSelecionar?.(sinal.ocorrencia.id);
      focar(sinal.x, sinal.y, 6);
    },
    [aoSelecionar, focar],
  );

  // Clique no chão: na prévia filtra pela zona; na tela cheia fecha a ocorrência aberta
  function aoClicarNoMapa(evento) {
    if (!dados || evento.target.closest(".sinal3d")) return;
    if (tela) {
      if (selecionada) aoSelecionar?.(null);
      return;
    }
    if (!aoSelecionarZona) return;
    const ponto = camera.pontoNaTela(evento.clientX, evento.clientY);
    const zona = zonaNoPonto(dados, ponto.x, ponto.y);
    if (zona) aoSelecionarZona(zonaSelecionada === zona ? null : zona);
  }

  function alternarTelaCheia() {
    const alvo = refVista.current?.closest("[data-tela-cheia]") ?? refVista.current;
    if (document.fullscreenElement) {
      document.exitFullscreen?.();
    } else {
      alvo?.requestFullscreen?.().catch(() => {});
    }
  }

  // Esc fecha a ocorrência aberta
  useEffect(() => {
    if (!tela || !selecionada) return undefined;
    const aoTeclar = (evento) => evento.key === "Escape" && aoSelecionar?.(null);
    window.addEventListener("keydown", aoTeclar);
    return () => window.removeEventListener("keydown", aoTeclar);
  }, [tela, selecionada, aoSelecionar]);

  const classes = [
    "mapa3d",
    `mapa3d--${modo}`,
    pronto ? "mapa3d--pronto" : "",
    vista.em3d ? "" : "mapa3d--plano",
    selecionada ? "mapa3d--com-selecao" : "",
  ].join(" ");

  return (
    <div ref={refMoldura} className={`mapa3d__moldura mapa3d__moldura--${modo}`}>
      <div
        ref={refVista}
        className={classes}
        tabIndex={tela ? 0 : -1}
        role="region"
        aria-roledescription="mapa"
        aria-label={
          tela
            ? "Mapa de São Paulo com as ocorrências ativas. Arraste para mover, use a roda ou a pinça para aproximar e o botão direito para girar. No teclado: setas movem, + e − aproximam, Q e E giram, T alterna 2D e 3D."
            : "Mapa de São Paulo com as ocorrências ativas. Clique numa zona para filtrar a lista."
        }
        onKeyDown={camera.aoTeclar}
        onClick={aoClicarNoMapa}
      >
        <canvas ref={refCanvas} className="mapa3d__canvas" aria-hidden="true" />

        <div className="mapa3d__palco">
          <div className="mapa3d__sinais">
            {sinais.map((sinal, indice) => (
              <SinalMapa
                key={sinal.ocorrencia.id}
                sinal={sinal}
                indice={indice}
                tela={tela}
                ativo={sinal.ocorrencia.id === selecionada}
                rotulo={rotulo}
                aoEscolher={escolherSinal}
              />
            ))}
          </div>
        </div>
        <span className="mapa3d__nevoa" aria-hidden="true" />

        {!dados && (
          <div className="mapa3d__carregando" role={erroMapa ? "alert" : "status"}>
            {erroMapa ? (
              <>
                <p>{erroMapa.message}</p>
                <button
                  type="button"
                  className="botao botao--secundario botao--pequeno"
                  onClick={() => setTentativa((n) => n + 1)}
                >
                  Tentar de novo
                </button>
              </>
            ) : (
              <p>
                <span className="carregando__marca" aria-hidden="true" />
                Carregando o mapa de São Paulo…
              </p>
            )}
          </div>
        )}
      </div>

      {tela && (
        <div className="mapa3d__controles" role="toolbar" aria-label="Câmera do mapa">
          <button type="button" className="mapa3d__botao" onClick={() => camera.zoomBotao(1.6)} title="Aproximar">
            <Icone nome="MAIS" tamanho={20} />
            <span className="visualmente-oculto">Aproximar</span>
          </button>
          <button type="button" className="mapa3d__botao" onClick={() => camera.zoomBotao(1 / 1.6)} title="Afastar">
            <Icone nome="MENOS" tamanho={20} />
            <span className="visualmente-oculto">Afastar</span>
          </button>
          <button
            type="button"
            className="mapa3d__botao mapa3d__bussola"
            onClick={camera.apontarNorte}
            title="Apontar para o norte"
          >
            <span className="mapa3d__agulha" aria-hidden="true">
              <span>N</span>
            </span>
            <span className="visualmente-oculto">Apontar para o norte</span>
          </button>
          <button
            type="button"
            className="mapa3d__botao mapa3d__botao--texto"
            onClick={camera.alternar3d}
            aria-pressed={vista.em3d}
            title={vista.em3d ? "Ver de cima (2D)" : "Ver em 3D"}
          >
            {vista.em3d ? "2D" : "3D"}
            <span className="visualmente-oculto">{vista.em3d ? ": ver o mapa de cima" : ": inclinar o mapa"}</span>
          </button>
          <button type="button" className="mapa3d__botao" onClick={() => verCidade()} title="Ver a cidade toda">
            <Icone nome="ENQUADRAR" tamanho={20} />
            <span className="visualmente-oculto">Ver a cidade toda</span>
          </button>
          <button type="button" className="mapa3d__botao" onClick={alternarTelaCheia} title="Tela cheia">
            <Icone nome="TELA_CHEIA" tamanho={20} />
            <span className="visualmente-oculto">Alternar tela cheia</span>
          </button>
        </div>
      )}

      <div className="mapa3d__legenda">
        <ul className="mapa3d__niveis" aria-label="Cores do risco">
          {NIVEIS.map((nivel) => (
            <li key={nivel}>
              <span className={`mapa3d__amostra mapa3d__amostra--${nivel}`} aria-hidden="true" />
              {rotulo("niveisRisco", nivel)}
            </li>
          ))}
          {indicesTrajeto.length > 0 && (
            <li>
              <span className="mapa3d__amostra-trajeto" aria-hidden="true" />
              Seu trajeto
            </li>
          )}
        </ul>
        {foraDoMapa > 0 && (
          <p className="mapa3d__aviso">
            {foraDoMapa} {foraDoMapa === 1 ? "ocorrência está" : "ocorrências estão"} em regiões fora do mapa.
          </p>
        )}
      </div>

      <p className="mapa3d__creditos">
        Mapa:{" "}
        <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noreferrer">
          © colaboradores do OpenStreetMap
        </a>
      </p>
    </div>
  );
}

// ------------------------------------------------- placa de advertência 3D

const SinalMapa = memo(function SinalMapa({ sinal, indice, tela, ativo, rotulo, aoEscolher }) {
  const { ocorrencia } = sinal;
  const { risco } = ocorrencia;
  const nivel = rotulo("niveisRisco", risco.nivel);
  const descricao = `${ocorrencia.titulo}. ${rotulo("tiposOcorrencia", ocorrencia.tipo)}. Risco ${nivel.toLowerCase()}, ${formatarNumero(risco.valor, 1)}.`;

  const faces = (
    <>
      <span className="sinal3d__chapa sinal3d__chapa--verso" />
      <span className="sinal3d__chapa sinal3d__chapa--miolo" />
      <span className="sinal3d__chapa sinal3d__chapa--frente">
        <Icone nome={ocorrencia.tipo} tamanho={17} className="sinal3d__icone" />
      </span>
    </>
  );

  return (
    <div
      className={`sinal3d sinal3d--${risco.nivel} ${ativo ? "sinal3d--ativo" : ""}`}
      style={{ left: sinal.x, top: sinal.y, "--i": indice, "--raio": `${16 + ocorrencia.gravidade * 6}px` }}
    >
      <div className="sinal3d__chao">
        <span className="sinal3d__anel" />
        {risco.alerta && <span key={`p-${risco.valor}`} className="sinal3d__pulso" />}
        <span className="sinal3d__sombra" />
      </div>
      <div className="sinal3d__painel">
        <div key={`e-${risco.nivel}`} className="sinal3d__ergue">
          <span className="sinal3d__haste" />
          {tela ? (
            <button
              type="button"
              className="sinal3d__placa"
              aria-label={descricao}
              aria-pressed={ativo}
              onClick={() => aoEscolher(sinal)}
            >
              {faces}
            </button>
          ) : (
            <a className="sinal3d__placa" href={`#/ocorrencias/${ocorrencia.id}`} aria-label={descricao}>
              {faces}
            </a>
          )}
          <span className="sinal3d__balao" aria-hidden="true">
            <strong>{ocorrencia.titulo}</strong>
            <span>
              Risco {nivel.toLowerCase()} · {formatarNumero(risco.valor, 1)}
            </span>
          </span>
        </div>
      </div>
    </div>
  );
});
