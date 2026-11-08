// Desenha o mapa no canvas, quadro a quadro, com a câmera atual.
//
// Ordem das camadas (como num app de mapa): chão da cidade, verde, água,
// aeroportos, trilhos, ruas (da menor para a maior), trajeto do cidadão, vias com
// ocorrência, prédios em 3D e, por cima de tudo, os nomes. Os nomes são escolhidos
// a cada quadro e não podem se sobrepor: os mais importantes entram primeiro.
//
// Para ficar rápido: só entra o que está na tela (teste pela caixa de cada feição),
// as ruas pequenas só aparecem de perto, de longe usa-se uma versão simplificada
// das avenidas e pontos a menos de 1 px do anterior são pulados.

import { criarProjecao, desprojetar } from "./projecao.js";

const COR = {
  fundo: "#e7eae8",
  cidade: "#f5f3ee",
  limite: "#9aa5aa",
  agua: "#a4cbec",
  aguaLinha: "#9cc6ea",
  verde: ["#c9e5c0", "#b9dbab", "#dcedd3"],
  aeroporto: "#e2e3e8",
  pista: "#b7bcc3",
  trilho: "#aeb5bc",
  trajeto: "#2d6fd0",
  texto: "#36414b",
  halo: "rgba(255, 255, 255, 0.94)",
  bairro: "#6f7b86",
  aguaTexto: "#3b78b5",
  verdeTexto: "#3d7a37",
  zonaTexto: "#1d2730",
  selecao: "#0f4c9b",
};

// Classes: 0 expressa, 1 arterial, 2 principal, 3 secundária, 4 terciária, 5 local, 6 pedestre, 7 acesso
const COR_VIA = [
  { pista: "#f6c35a", borda: "#d99f3e" },
  { pista: "#fcd877", borda: "#e1b456" },
  { pista: "#ffffff", borda: "#d0c9ba" },
  { pista: "#ffffff", borda: "#d6d0c4" },
  { pista: "#ffffff", borda: "#dcd7cc" },
  { pista: "#ffffff", borda: "#e0ddd5" },
  { pista: "#f2efe8", borda: "#e0ddd5" },
  { pista: "#fde39c", borda: "#e2c07a" },
];

// Largura da pista em px nos zooms 0,25 · 1 · 4 · 16 (cresce devagar, como nos apps de mapa)
const LARGURA_VIA = [
  [1.7, 3.8, 9, 22],
  [1.5, 3.4, 8, 20],
  [0.9, 2.7, 7, 17],
  [0.6, 2.1, 6, 15],
  [0.4, 1.4, 4.8, 13],
  [0.3, 0.9, 3.4, 11],
  [0.3, 0.7, 2.3, 7],
  [0.5, 1.3, 3.8, 10],
];

// A partir de qual zoom cada classe aparece, e a partir de qual o nome dela aparece
const ZOOM_VIA = [0, 0, 0, 0, 0.7, 1.3, 3, 1];
const ZOOM_NOME = [0.45, 0.55, 0.85, 1.4, 2.2, 3.4, 5, Infinity];
const ORDEM_DESENHO = [6, 5, 7, 4, 3, 2, 1, 0];
const FONTE_NOME = [12.5, 12.5, 12, 11.5, 11, 10.5, 10.5, 10];

const COR_RISCO = { BAIXO: "#2f7d4e", MODERADO: "#e2a400", ALTO: "#e2681c", CRITICO: "#c42b2b" };

function interpolar(tabela, zoom) {
  const posicao = Math.min(Math.max((Math.log2(zoom) + 2) / 2, 0), 3);
  const i = Math.min(Math.floor(posicao), 2);
  const f = posicao - i;
  return tabela[i] * (1 - f) + tabela[i + 1] * f;
}

export function larguraVia(classe, zoom) {
  return interpolar(LARGURA_VIA[classe], zoom);
}

// --------------------------------------------------------------- ferramentas

function intercepta(caixa, area) {
  return caixa[2] >= area.x0 && caixa[0] <= area.x1 && caixa[3] >= area.y0 && caixa[1] <= area.y1;
}

/** Acrescenta uma linha (já projetada na tela) ao caminho, pulando pontos a menos de 0,8 px. */
function tracar(caminho, pontos, proj, fechar = false) {
  let comecou = false;
  let ultimoX = 0;
  let ultimoY = 0;
  const n = pontos.length;
  for (let i = 0; i < n; i += 2) {
    const p = proj.projetar(pontos[i], pontos[i + 1]);
    if (!p.ok) {
      comecou = false;
      continue;
    }
    if (!comecou) {
      caminho.moveTo(p.x, p.y);
      comecou = true;
      ultimoX = p.x;
      ultimoY = p.y;
    } else if (i === n - 2 || Math.abs(p.x - ultimoX) + Math.abs(p.y - ultimoY) > 0.8) {
      caminho.lineTo(p.x, p.y);
      ultimoX = p.x;
      ultimoY = p.y;
    }
  }
  if (fechar && comecou) caminho.closePath();
}

/** Fator de perspectiva no centro de uma caixa: perto da câmera > 1, longe < 1. */
function fatorNaCaixa(caixa, proj) {
  return proj.projetar((caixa[0] + caixa[2]) / 2, (caixa[1] + caixa[3]) / 2).k;
}

const larguras = new Map();
function medir(ctx, fonte, texto) {
  const chave = `${fonte}|${texto}`;
  let largura = larguras.get(chave);
  if (largura === undefined) {
    ctx.font = fonte;
    largura = ctx.measureText(texto).width;
    larguras.set(chave, largura);
  }
  return largura;
}

// ------------------------------------------------------------------- desenho

/**
 * @param ctx contexto 2D do canvas (já do tamanho da tela × densidade de pixels)
 * @param estado { dados, camera, largura, altura, ox, oy, dpr, destaque, memoria }
 *   destaque: { afetadas: [{ indices, nivel, impacto, forte, fraca }], trajeto: [indices],
 *               zonaSelecionada, riscoZona: { CHAVE: nivel } }
 *   memoria: objeto guardado entre quadros (nomes escritos no quadro anterior)
 */
export function desenharMapa(ctx, estado) {
  const { dados, camera, largura, altura, ox, oy, dpr, destaque = {}, memoria = {} } = estado;
  const { zoom } = camera;
  const proj = criarProjecao(camera, ox, oy);

  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.fillStyle = COR.fundo;
  ctx.fillRect(0, 0, largura, altura);
  ctx.lineCap = "round";
  ctx.lineJoin = "round";

  // Área do chão visível: os quatro cantos da tela levados para o plano
  const cantos = [
    [-ox, -oy],
    [largura - ox, -oy],
    [-ox, altura - oy],
    [largura - ox, altura - oy],
  ].map(([sx, sy]) => desprojetar(camera, sx, sy));
  const margem = 40 / zoom;
  const area = {
    x0: Math.min(...cantos.map((c) => c.x)) - margem,
    y0: Math.min(...cantos.map((c) => c.y)) - margem,
    x1: Math.max(...cantos.map((c) => c.x)) + margem,
    y1: Math.max(...cantos.map((c) => c.y)) + margem,
  };

  // 1. Chão da cidade
  ctx.beginPath();
  dados.limite.forEach((anel) => tracar(ctx, anel, proj, true));
  ctx.fillStyle = COR.cidade;
  ctx.fill("evenodd");

  // Zonas com ocorrência ganham um leve tom do pior risco, que some quando a câmera aproxima
  const tom = 0.07 * Math.min(Math.max((1.6 - zoom) / 1.1, 0), 1);
  dados.zonas.forEach((zona) => {
    const nivel = destaque.riscoZona?.[zona.chave];
    if (!nivel || tom <= 0) return;
    ctx.beginPath();
    zona.aneis.forEach((anel) => tracar(ctx, anel, proj, true));
    ctx.globalAlpha = tom;
    ctx.fillStyle = COR_RISCO[nivel];
    ctx.fill();
    ctx.globalAlpha = 1;
  });

  // 2. Verde
  dados.verde.forEach((parque) => {
    if (!intercepta(parque.caixa, area)) return;
    ctx.beginPath();
    parque.aneis.forEach((anel) => tracar(ctx, anel, proj, true));
    ctx.fillStyle = COR.verde[parque.tipo] ?? COR.verde[0];
    ctx.fill("evenodd");
  });

  // 3. Água: rios estreitos como linhas e, por cima, rios largos, lagos e represas
  ctx.strokeStyle = COR.aguaLinha;
  dados.rios.forEach((rio) => {
    if (!intercepta(rio.caixa, area)) return;
    const k = fatorNaCaixa(rio.caixa, proj);
    ctx.lineWidth = Math.max(0.8, (rio.tipo ? 1.6 : 0.8) * Math.sqrt(zoom) * k);
    ctx.beginPath();
    tracar(ctx, rio.pontos, proj);
    ctx.stroke();
  });
  ctx.fillStyle = COR.agua;
  dados.agua.forEach((lago) => {
    if (!intercepta(lago.caixa, area)) return;
    ctx.beginPath();
    lago.aneis.forEach((anel) => tracar(ctx, anel, proj, true));
    ctx.fill("evenodd");
  });

  // 4. Aeroportos
  dados.aeroportos.forEach((item) => {
    if (!intercepta(item.caixa, area)) return;
    ctx.beginPath();
    tracar(ctx, item.pontos, proj, item.tipo === 0);
    if (item.tipo === 0) {
      ctx.fillStyle = COR.aeroporto;
      ctx.fill();
    } else {
      ctx.strokeStyle = COR.pista;
      ctx.lineWidth = Math.max(1.5, 1.8 * zoom * fatorNaCaixa(item.caixa, proj));
      ctx.lineCap = "butt";
      ctx.stroke();
      ctx.lineCap = "round";
    }
  });

  // 5. Trilhos (trem e monotrilho na superfície)
  if (zoom >= 0.6) {
    const caminho = new Path2D();
    dados.trilhos.forEach((trilho) => {
      if (intercepta(trilho.caixa, area)) tracar(caminho, trilho.pontos, proj);
    });
    ctx.strokeStyle = COR.trilho;
    ctx.lineWidth = zoom >= 2 ? 3 : 1.6;
    ctx.stroke(caminho);
    if (zoom >= 2) {
      ctx.strokeStyle = "#ffffff";
      ctx.lineWidth = 1.4;
      ctx.setLineDash([6, 6]);
      ctx.stroke(caminho);
      ctx.setLineDash([]);
    }
  }

  // 6. Ruas: de longe, a versão leve das avenidas; de perto, todas as classes visíveis
  const deLonge = zoom < 0.55;
  const porClasse = ORDEM_DESENHO.map(() => new Map());
  const juntar = (classe, pontos, caixaVia) => {
    const k = fatorNaCaixa(caixaVia, proj);
    // Rua local muito longe da câmera (perto do horizonte) não aparece
    if (classe >= 5 && k < 0.55) return;
    if (classe === 4 && k < 0.4) return;
    const faixa = Math.round(Math.min(Math.max(k, 0.35), 2.2) * 5) / 5;
    const grupos = porClasse[classe];
    if (!grupos.has(faixa)) grupos.set(faixa, new Path2D());
    tracar(grupos.get(faixa), pontos, proj);
  };
  if (deLonge) {
    dados.macro.forEach((via) => {
      if (intercepta(via.caixa, area)) juntar(via.classe, via.pontos, via.caixa);
    });
  } else {
    dados.ruas.forEach((via) => {
      if (zoom < ZOOM_VIA[via.classe] || !intercepta(via.caixa, area)) return;
      juntar(via.classe, via.pontos, via.caixa);
    });
  }
  ORDEM_DESENHO.forEach((classe) => {
    const grupos = porClasse[classe];
    if (!grupos.size) return;
    const base = larguraVia(classe, zoom);
    // Contorno
    ctx.strokeStyle = COR_VIA[classe].borda;
    grupos.forEach((caminho, faixa) => {
      const pista = base * faixa;
      ctx.lineWidth = pista < 1.4 ? Math.max(pista, 0.6) : pista + Math.min(Math.max(pista * 0.28, 1), 2.6);
      ctx.stroke(caminho);
    });
    // Pista (as vias finas de longe ficam só com o contorno, como linhas cinzas)
    ctx.strokeStyle = COR_VIA[classe].pista;
    grupos.forEach((caminho, faixa) => {
      const pista = base * faixa;
      if (pista < 1.4) return;
      ctx.lineWidth = pista;
      ctx.stroke(caminho);
    });
  });

  // 7. Trajeto do cidadão e vias com ocorrência
  const desenharDestaque = (indices, cor, { brilho, nucleo, tracejado, alfa = 1 }) => {
    const caminho = new Path2D();
    let k = 1;
    indices.forEach((indice) => {
      const via = dados.ruas[indice];
      if (!intercepta(via.caixa, area)) return;
      k = fatorNaCaixa(via.caixa, proj);
      tracar(caminho, via.pontos, proj);
    });
    const escala = Math.min(Math.max(k, 0.5), 1.8);
    ctx.globalAlpha = 0.26 * alfa;
    ctx.strokeStyle = cor;
    ctx.lineWidth = brilho * escala;
    ctx.stroke(caminho);
    ctx.globalAlpha = alfa;
    ctx.lineWidth = nucleo * escala;
    if (tracejado) ctx.setLineDash([nucleo * 1.6, nucleo * 1.4]);
    ctx.stroke(caminho);
    ctx.setLineDash([]);
    ctx.globalAlpha = 1;
  };
  const larguraDestaque = Math.max(3.2, larguraVia(2, zoom) * 0.75);
  if (destaque.trajeto?.length) {
    desenharDestaque(destaque.trajeto, COR.trajeto, { brilho: larguraDestaque + 9, nucleo: larguraDestaque });
  }
  (destaque.afetadas ?? []).forEach((item) => {
    desenharDestaque(item.indices, COR_RISCO[item.nivel], {
      brilho: larguraDestaque + (item.forte ? 18 : 11),
      nucleo: larguraDestaque * (item.forte ? 1.25 : 1),
      tracejado: item.impacto === "LENTIDAO" || item.impacto === "SEM_INTERDICAO",
      alfa: item.fraca ? 0.35 : 1,
    });
  });

  // 8. Zona escolhida: as outras ficam esmaecidas e a escolhida ganha contorno azul
  if (destaque.zonaSelecionada) {
    ctx.beginPath();
    dados.zonas
      .filter((zona) => zona.chave !== destaque.zonaSelecionada)
      .forEach((zona) => zona.aneis.forEach((anel) => tracar(ctx, anel, proj, true)));
    ctx.fillStyle = "rgba(245, 246, 246, 0.55)";
    ctx.fill();
    const escolhida = dados.zonas.find((zona) => zona.chave === destaque.zonaSelecionada);
    ctx.beginPath();
    escolhida?.aneis.forEach((anel) => tracar(ctx, anel, proj, true));
    ctx.strokeStyle = COR.selecao;
    ctx.lineWidth = 3;
    ctx.stroke();
  }

  // Divisa do município, tracejada
  ctx.beginPath();
  dados.limite.forEach((anel) => tracar(ctx, anel, proj, true));
  ctx.strokeStyle = COR.limite;
  ctx.lineWidth = 1.4;
  ctx.setLineDash([7, 5]);
  ctx.stroke();
  ctx.setLineDash([]);

  // 9. Prédios em 3D (de perto)
  if (zoom >= 2.4 && camera.incl > 5) {
    desenharPredios(ctx, dados, proj, area, Math.min((zoom - 2.4) / 0.8, 1), zoom);
  }

  // 10. Nomes (desviando das placas das ocorrências, que ficam por cima do canvas)
  desenharNomes(ctx, dados, proj, area, { zoom, largura, altura, memoria, obstaculos: destaque.obstaculos ?? [] });
}

// ------------------------------------------------------------------ prédios

const EXAGERO_ALTURA = 1.4;

function desenharPredios(ctx, dados, proj, area, opacidade, zoom) {
  const visiveis = [];
  dados.predios.forEach((predio) => {
    if (!intercepta(predio.caixa, area)) return;
    const p = proj.projetar(predio.cx, predio.cy);
    if (!p.ok || p.k < 0.6) return;
    // Prédio que ocupa menos de 5 px na tela não vale o desenho
    const tamanho = Math.max(predio.caixa[2] - predio.caixa[0], predio.caixa[3] - predio.caixa[1]) * zoom * p.k;
    if (tamanho < 5) return;
    visiveis.push({ predio, profundidade: proj.profundidade(predio.cx, predio.cy) });
  });
  // Algoritmo do pintor: primeiro os mais distantes, para os da frente ficarem por cima
  visiveis.sort((a, b) => a.profundidade - b.profundidade);
  const { paraCamera } = proj;
  ctx.globalAlpha = opacidade;
  ctx.lineWidth = 0.6;
  ctx.strokeStyle = "rgba(60, 64, 70, 0.25)";
  const base = [];
  const topo = [];
  visiveis.forEach(({ predio }) => {
    const { pontos, normais, noSol } = predio;
    const n = pontos.length / 2;
    const h = predio.altura * EXAGERO_ALTURA;
    for (let i = 0; i < n; i += 1) {
      const b = proj.projetar(pontos[i * 2], pontos[i * 2 + 1], 0);
      base[i * 2] = b.x;
      base[i * 2 + 1] = b.y;
      const t = proj.projetar(pontos[i * 2], pontos[i * 2 + 1], h);
      topo[i * 2] = t.x;
      topo[i * 2 + 1] = t.y;
    }
    // Só as paredes viradas para a câmera (as de trás ficam escondidas pelo próprio prédio),
    // em dois grupos: as que pegam sol (mais claras) e as da sombra. Dois preenchimentos por prédio.
    for (const lado of [0, 1]) {
      ctx.beginPath();
      for (let i = 0; i < n; i += 1) {
        if (noSol[i] !== lado) continue;
        if (normais[i * 2] * paraCamera.x + normais[i * 2 + 1] * paraCamera.y < -0.15) continue;
        const j = (i + 1) % n;
        ctx.moveTo(base[i * 2], base[i * 2 + 1]);
        ctx.lineTo(base[j * 2], base[j * 2 + 1]);
        ctx.lineTo(topo[j * 2], topo[j * 2 + 1]);
        ctx.lineTo(topo[i * 2], topo[i * 2 + 1]);
        ctx.closePath();
      }
      ctx.fillStyle = lado ? "#dedbd4" : "#bfbbb3";
      ctx.fill();
    }
    ctx.beginPath();
    ctx.moveTo(topo[0], topo[1]);
    for (let i = 1; i < n; i += 1) ctx.lineTo(topo[i * 2], topo[i * 2 + 1]);
    ctx.closePath();
    ctx.fillStyle = "#eeece7";
    ctx.fill();
    ctx.stroke();
  });
  ctx.globalAlpha = 1;
}

// -------------------------------------------------------------------- nomes

function sobrepoe(caixa, ocupadas) {
  for (let i = 0; i < ocupadas.length; i += 1) {
    const o = ocupadas[i];
    if (caixa[0] < o[2] && caixa[2] > o[0] && caixa[1] < o[3] && caixa[3] > o[1]) return true;
  }
  return false;
}

function desenharNomes(ctx, dados, proj, area, { zoom, largura, altura, memoria, obstaculos }) {
  const candidatos = [];
  const naTela = (x, y) => x >= area.x0 && x <= area.x1 && y >= area.y0 && y <= area.y1;
  const anteriores = memoria.nomes ?? new Set();

  // Nomes das zonas, de longe
  if (zoom < 0.75) {
    dados.zonas.forEach((zona) =>
      candidatos.push({ id: `z${zona.chave}`, prioridade: 0, tipo: "zona", texto: zona.nome.toUpperCase(), x: zona.centro[0], y: zona.centro[1] }),
    );
  }
  // Represas, parques grandes e aeroportos
  dados.rotulosAreas.forEach((item) => {
    if (!naTela(item.x, item.y)) return;
    const tamanhoNaTela = item.area * zoom * zoom;
    if (tamanhoNaTela < 1800 || (zoom < 0.45 && item.area < 3000)) return;
    candidatos.push({ id: `a${item.id}`, prioridade: item.tipo === 0 ? 1 : 3, tipo: ["agua", "verde", "aero"][item.tipo], texto: item.texto, x: item.x, y: item.y });
  });
  // Bairros
  dados.lugares.forEach((lugar) => {
    const minimo = [0.8, 1.6, 2.2][lugar.tipo];
    if (zoom < minimo || zoom > 9 || !naTela(lugar.x, lugar.y)) return;
    candidatos.push({ id: `b${lugar.id}`, prioridade: [2.5, 4.5, 5.5][lugar.tipo], tipo: "bairro", nivel: lugar.tipo, texto: lugar.texto.toUpperCase(), x: lugar.x, y: lugar.y });
  });
  // Ruas
  dados.rotulos.forEach((rotulo) => {
    if (zoom < ZOOM_NOME[rotulo.classe] || !naTela(rotulo.x, rotulo.y)) return;
    candidatos.push({ id: `r${rotulo.id}`, prioridade: [2, 2, 3, 4, 5, 6, 7, 9][rotulo.classe], tipo: "rua", rotulo, texto: rotulo.texto, x: rotulo.x, y: rotulo.y });
  });

  // Os mais importantes primeiro; entre iguais, os que já estavam na tela (evita piscar)
  candidatos.sort(
    (a, b) => a.prioridade - b.prioridade || (anteriores.has(b.id) ? 1 : 0) - (anteriores.has(a.id) ? 1 : 0),
  );

  // Cada placa ocupa um retângulo na tela: do pé do poste até o topo da placa (mesma escala da câmera)
  const crescimento = Math.min(Math.max(0.95 + 0.12 * Math.log2(zoom), 0.75), 1.35);
  const ocupadas = obstaculos
    .map(([ox2, oy2]) => {
      const p = proj.projetar(ox2, oy2);
      const alto = 82 * crescimento * p.k;
      const meio = 24 * crescimento * p.k;
      return p.ok ? [p.x - meio, p.y - alto, p.x + meio, p.y + 8] : null;
    })
    .filter(Boolean);
  const escritos = new Set();
  // O mesmo nome não se repete a menos de 320 px na tela (avenidas e rodovias longas)
  const posicoesPorNome = new Map();
  const pertoDeOutroIgual = (texto, x, y) =>
    (posicoesPorNome.get(texto) ?? []).some(([px, py]) => Math.hypot(px - x, py - y) < 320);
  const lembrar = (texto, x, y) => {
    if (!posicoesPorNome.has(texto)) posicoesPorNome.set(texto, []);
    posicoesPorNome.get(texto).push([x, y]);
  };
  const limite = 420;
  if ("letterSpacing" in ctx) ctx.letterSpacing = "0px";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.lineJoin = "round";

  for (const item of candidatos) {
    if (escritos.size >= limite) break;
    const p = proj.projetar(item.x, item.y);
    if (!p.ok || p.k < 0.5 || p.x < -50 || p.y < 10 || p.x > largura + 50 || p.y > altura + 50) continue;
    const x = p.x;
    const y = p.y;

    if (item.tipo === "rua") {
      const { rotulo } = item;
      const tamanho = FONTE_NOME[rotulo.classe] * Math.min(Math.max(p.k, 0.85), 1.15);
      const fonte = `${rotulo.classe <= 2 ? 600 : 500} ${tamanho.toFixed(1)}px "Public Sans", system-ui, sans-serif`;
      const larguraTexto = medir(ctx, fonte, item.texto);
      // Direção da rua na tela
      const q = proj.projetar(rotulo.x + Math.cos(rotulo.angulo) * 4, rotulo.y + Math.sin(rotulo.angulo) * 4);
      let angulo = Math.atan2(q.y - y, q.x - x);
      if (angulo > Math.PI / 2) angulo -= Math.PI;
      if (angulo < -Math.PI / 2) angulo += Math.PI;
      const cos = Math.abs(Math.cos(angulo));
      const sen = Math.abs(Math.sin(angulo));
      const meiaL = (larguraTexto * cos + tamanho * sen) / 2 + 3;
      const meiaA = (larguraTexto * sen + tamanho * cos) / 2 + 3;
      const caixaTexto = [x - meiaL, y - meiaA, x + meiaL, y + meiaA];
      if (pertoDeOutroIgual(item.texto, x, y) || sobrepoe(caixaTexto, ocupadas)) continue;
      ocupadas.push(caixaTexto);
      escritos.add(item.id);
      lembrar(item.texto, x, y);
      ctx.save();
      ctx.translate(x, y);
      ctx.rotate(angulo);
      ctx.font = fonte;
      ctx.lineWidth = 3.4;
      ctx.strokeStyle = COR.halo;
      ctx.strokeText(item.texto, 0, 0);
      ctx.fillStyle = COR.texto;
      ctx.fillText(item.texto, 0, 0);
      ctx.restore();
      continue;
    }

    const estilos = {
      zona: { fonte: '800 15px "Overpass", system-ui, sans-serif', cor: COR.zonaTexto, espaco: "3px" },
      bairro: {
        fonte: `${item.nivel === 0 ? 700 : 600} ${item.nivel === 0 ? 11.5 : 10.5}px "Public Sans", system-ui, sans-serif`,
        cor: COR.bairro,
        espaco: "1.2px",
      },
      agua: { fonte: 'italic 500 12px "Public Sans", system-ui, sans-serif', cor: COR.aguaTexto, espaco: "0px" },
      verde: { fonte: '600 11.5px "Public Sans", system-ui, sans-serif', cor: COR.verdeTexto, espaco: "0px" },
      aero: { fonte: '600 11.5px "Public Sans", system-ui, sans-serif', cor: "#5f6670", espaco: "0px" },
    };
    const estilo = estilos[item.tipo];
    const espaco = "letterSpacing" in ctx ? Number.parseFloat(estilo.espaco) : 0;
    const larguraTexto = medir(ctx, estilo.fonte, item.texto) + item.texto.length * espaco;
    const altTexto = item.tipo === "zona" ? 16 : 12;
    const caixaTexto = [x - larguraTexto / 2 - 4, y - altTexto / 2 - 3, x + larguraTexto / 2 + 4, y + altTexto / 2 + 3];
    if (sobrepoe(caixaTexto, ocupadas)) continue;
    ocupadas.push(caixaTexto);
    escritos.add(item.id);
    if ("letterSpacing" in ctx) ctx.letterSpacing = estilo.espaco;
    ctx.font = estilo.fonte;
    ctx.lineWidth = item.tipo === "zona" ? 4.5 : 3.4;
    ctx.strokeStyle = COR.halo;
    ctx.strokeText(item.texto, x, y);
    ctx.fillStyle = estilo.cor;
    ctx.fillText(item.texto, x, y);
    if ("letterSpacing" in ctx) ctx.letterSpacing = "0px";
  }
  memoria.nomes = escritos;
}
