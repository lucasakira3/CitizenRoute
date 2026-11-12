// Cálculo de rotas no próprio navegador, sem serviço externo.
//
// A rede das vias principais de São Paulo (public/mapa/sao-paulo-rotas.json, criada por
// scripts/gerar-mapa.mjs a partir do OpenStreetMap) é um grafo: cada cruzamento é um nó e
// cada pedaço de via entre dois cruzamentos é uma aresta, com comprimento, classe, nome e
// sentido. A rota é o caminho de menor custo entre dois nós, achado com o algoritmo A*.
//
// O custo de uma aresta é o tempo para percorrê-la. Para a "rota mais segura", as arestas
// das vias com ocorrência de risco moderado ou maior para o modal ficam muito mais caras:
// o caminho desvia delas sempre que existe alternativa.
//
// Plano do mapa: x cresce para o leste e y para o sul, em unidades de 25 m.

import { chaveVia, tracadoDaVia, zonaNoPonto } from "./mapaCidade.js";

// Velocidade de referência por classe de via (km/h), para veículos. A ordem segue CLASSES do
// gerador: expressa, arterial, principal, secundária, terciária, local, pedestre, acesso.
const VELOCIDADE_CLASSE = [60, 50, 40, 35, 30, 25, 5, 35];
const EXPRESSA = 0;

// Quanto mais caro fica passar por uma via com ocorrência, conforme o risco para o modal
export const PENALIDADE_RISCO = { BAIXO: 1, MODERADO: 6, ALTO: 25, CRITICO: 80 };
const ORDEM_RISCO = { BAIXO: 1, MODERADO: 2, ALTO: 3, CRITICO: 4 };

// ------------------------------------------------------------- carregamento

let promessa = null;

/** Carrega a rede uma vez só (cerca de 2 MB); as próximas chamadas reaproveitam. */
export function carregarRede() {
  if (!promessa) {
    promessa = fetch(`${import.meta.env.BASE_URL}mapa/sao-paulo-rotas.json`)
      .then((resposta) => {
        if (!resposta.ok) throw new Error("Não foi possível carregar a rede de ruas para calcular rotas.");
        return resposta.json();
      })
      .then(prepararRede)
      .catch((erro) => {
        promessa = null;
        throw erro;
      });
  }
  return promessa;
}

function prepararRede(bruto) {
  const { escala, metrosPorUnidade } = bruto;
  const total = bruto.nos.length / 2;
  const nx = new Float32Array(total);
  const ny = new Float32Array(total);
  let x = 0;
  let y = 0;
  for (let i = 0; i < total; i += 1) {
    x += bruto.nos[i * 2];
    y += bruto.nos[i * 2 + 1];
    nx[i] = x / escala;
    ny[i] = y / escala;
  }

  const arestas = bruto.arestas.map(([a, b, classe, nome, sentido, comprimento, ...meio]) => {
    // Geometria completa do trecho: cruzamento a, pontos do meio e cruzamento b
    const pontos = new Float32Array(meio.length + 4);
    pontos[0] = nx[a];
    pontos[1] = ny[a];
    let qx = Math.round(nx[a] * escala);
    let qy = Math.round(ny[a] * escala);
    for (let i = 0; i < meio.length; i += 2) {
      qx += meio[i];
      qy += meio[i + 1];
      pontos[i + 2] = qx / escala;
      pontos[i + 3] = qy / escala;
    }
    pontos[pontos.length - 2] = nx[b];
    pontos[pontos.length - 1] = ny[b];
    return { a, b, classe, nome, maoUnica: sentido === 1, metros: (comprimento / 10) * metrosPorUnidade, pontos };
  });

  // Lista de vizinhos em formato compacto (CSR): para cada nó, as arestas que saem dele
  const grau = new Int32Array(total + 1);
  arestas.forEach(({ a, b }) => {
    grau[a + 1] += 1;
    grau[b + 1] += 1;
  });
  for (let i = 0; i < total; i += 1) grau[i + 1] += grau[i];
  const destino = new Int32Array(grau[total]);
  const aresta = new Int32Array(grau[total]);
  const contraMao = new Uint8Array(grau[total]);
  const posicao = grau.slice(0, total);
  arestas.forEach((item, indice) => {
    let p = posicao[item.a]++;
    destino[p] = item.b;
    aresta[p] = indice;
    p = posicao[item.b]++;
    destino[p] = item.a;
    aresta[p] = indice;
    contraMao[p] = item.maoUnica ? 1 : 0;
  });

  // Grade para achar o cruzamento mais próximo de um ponto sem olhar todos
  const CELULA = 40;
  const grade = new Map();
  for (let i = 0; i < total; i += 1) {
    const chave = `${Math.floor(nx[i] / CELULA)},${Math.floor(ny[i] / CELULA)}`;
    if (!grade.has(chave)) grade.set(chave, []);
    grade.get(chave).push(i);
  }

  return { nx, ny, arestas, inicio: grau, destino, aresta, contraMao, grade, CELULA, metrosPorUnidade };
}

/** Cruzamentos mais próximos de um ponto, do mais perto para o mais longe. */
export function nosProximos(rede, x, y, quantidade = 6) {
  const { grade, CELULA, nx, ny } = rede;
  const cx = Math.floor(x / CELULA);
  const cy = Math.floor(y / CELULA);
  for (let raio = 1; raio <= 12; raio += 1) {
    const candidatos = [];
    for (let i = cx - raio; i <= cx + raio; i += 1) {
      for (let j = cy - raio; j <= cy + raio; j += 1) {
        (grade.get(`${i},${j}`) ?? []).forEach((no) => candidatos.push(no));
      }
    }
    if (candidatos.length >= quantidade || raio === 12) {
      return candidatos
        .map((no) => ({ no, distancia: Math.hypot(nx[no] - x, ny[no] - y) }))
        .sort((p, q) => p.distancia - q.distancia)
        .slice(0, quantidade);
    }
  }
  return [];
}

// ------------------------------------------------------------------ cálculo

/** Fila de prioridade (heap binário) de nós pelo custo estimado. */
class Fila {
  constructor() {
    this.nos = [];
    this.custos = [];
  }

  get vazia() {
    return this.nos.length === 0;
  }

  colocar(no, custo) {
    const { nos, custos } = this;
    nos.push(no);
    custos.push(custo);
    let i = nos.length - 1;
    while (i > 0) {
      const pai = (i - 1) >> 1;
      if (custos[pai] <= custos[i]) break;
      [nos[pai], nos[i]] = [nos[i], nos[pai]];
      [custos[pai], custos[i]] = [custos[i], custos[pai]];
      i = pai;
    }
  }

  tirar() {
    const { nos, custos } = this;
    const no = nos[0];
    const ultimoNo = nos.pop();
    const ultimoCusto = custos.pop();
    if (nos.length) {
      nos[0] = ultimoNo;
      custos[0] = ultimoCusto;
      let i = 0;
      for (;;) {
        const e = i * 2 + 1;
        const d = e + 1;
        let menor = i;
        if (e < nos.length && custos[e] < custos[menor]) menor = e;
        if (d < nos.length && custos[d] < custos[menor]) menor = d;
        if (menor === i) break;
        [nos[menor], nos[i]] = [nos[i], nos[menor]];
        [custos[menor], custos[i]] = [custos[i], custos[menor]];
        i = menor;
      }
    }
    return no;
  }
}

/**
 * A* de "origem" até "destino" (índices de nós). Devolve a lista de passos
 * [{ aresta, deA }] ou null se não houver caminho.
 *   motorizado: respeita a mão única e pode usar vias expressas
 *   velocidade: km/h de quem anda a pé ou de bicicleta (veículos usam a da classe da via)
 *   penalidade: Map(nome → multiplicador do custo)
 */
function aEstrela(rede, origem, destino, { motorizado, velocidade, penalidade }) {
  const { nx, ny, arestas, inicio, destino: vizinho, aresta: arestaDe, contraMao, metrosPorUnidade } = rede;
  const total = nx.length;
  const custo = new Float64Array(total).fill(Infinity);
  const veioPor = new Int32Array(total).fill(-1);
  const fechado = new Uint8Array(total);
  // Heurística: distância em linha reta na maior velocidade possível (nunca superestima)
  const maisRapida = (motorizado ? Math.max(...VELOCIDADE_CLASSE) : velocidade) / 3.6;
  const estimar = (no) => (Math.hypot(nx[no] - nx[destino], ny[no] - ny[destino]) * metrosPorUnidade) / maisRapida;

  const fila = new Fila();
  custo[origem] = 0;
  fila.colocar(origem, estimar(origem));
  while (!fila.vazia) {
    const atual = fila.tirar();
    if (fechado[atual]) continue;
    if (atual === destino) break;
    fechado[atual] = 1;
    for (let p = inicio[atual]; p < inicio[atual + 1]; p += 1) {
      const outro = vizinho[p];
      if (fechado[outro]) continue;
      const item = arestas[arestaDe[p]];
      if (motorizado && contraMao[p]) continue;
      if (!motorizado && item.classe === EXPRESSA) continue;
      const kmh = motorizado ? VELOCIDADE_CLASSE[item.classe] : velocidade;
      const fator = penalidade?.get(item.nome) ?? 1;
      const novo = custo[atual] + (item.metros / (kmh / 3.6)) * fator;
      if (novo < custo[outro]) {
        custo[outro] = novo;
        veioPor[outro] = p;
        fila.colocar(outro, novo + estimar(outro));
      }
    }
  }
  if (!Number.isFinite(custo[destino])) return null;

  const passos = [];
  for (let no = destino; no !== origem; ) {
    const p = veioPor[no];
    const item = arestas[arestaDe[p]];
    const deA = item.b === no;
    passos.push({ aresta: arestaDe[p], deA });
    no = deA ? item.a : item.b;
  }
  return passos.reverse();
}

/**
 * Calcula a rota entre dois pontos do plano.
 * O ponto escolhido (uma rua pequena, um clique no mapa) é ligado ao cruzamento de via
 * principal mais próximo por um trecho de acesso, desenhado tracejado.
 *
 * Devolve { pontos, acessos, distanciaMetros, nomes } ou lança um erro com mensagem clara.
 */
export function calcularRota(rede, origem, destino, { motorizado, velocidade, penalidade }) {
  const perto = (ponto) => nosProximos(rede, ponto.x, ponto.y, 5);
  const saidas = perto(origem);
  const chegadas = perto(destino);
  if (!saidas.length || !chegadas.length) {
    throw new Error("Não encontramos vias principais perto de um dos pontos. Escolha um lugar dentro de São Paulo.");
  }

  // Tenta os cruzamentos mais próximos até achar um caminho (um pode ser saída de mão única)
  for (const saida of saidas) {
    for (const chegada of chegadas) {
      if (saida.no === chegada.no) continue;
      const passos = aEstrela(rede, saida.no, chegada.no, { motorizado, velocidade, penalidade });
      if (passos) return montarResultado(rede, origem, destino, saida, chegada, passos);
    }
  }
  throw new Error("Não foi possível ligar a saída ao destino pelas vias principais. Tente pontos mais próximos de uma avenida.");
}

function montarResultado(rede, origem, destino, saida, chegada, passos) {
  const { arestas, nx, ny, metrosPorUnidade } = rede;
  const linha = [nx[saida.no], ny[saida.no]];
  const nomes = [];
  // Trechos seguidos com o mesmo nome, para pintar só a parte da rota que tem ocorrência
  const segmentos = [];
  let metros = 0;
  passos.forEach(({ aresta, deA }) => {
    const { pontos, nome, metros: tamanho } = arestas[aresta];
    metros += tamanho;
    if (nome >= 0 && nomes[nomes.length - 1] !== nome) nomes.push(nome);
    let segmento = segmentos[segmentos.length - 1];
    if (!segmento || segmento.nome !== nome) {
      segmento = { nome, pontos: [linha[linha.length - 2], linha[linha.length - 1]] };
      segmentos.push(segmento);
    }
    const n = pontos.length / 2;
    for (let k = 1; k < n; k += 1) {
      const i = deA ? k : n - 1 - k;
      linha.push(pontos[i * 2], pontos[i * 2 + 1]);
      segmento.pontos.push(pontos[i * 2], pontos[i * 2 + 1]);
    }
  });
  const acessos = [
    [origem.x, origem.y, nx[saida.no], ny[saida.no]],
    [nx[chegada.no], ny[chegada.no], destino.x, destino.y],
  ];
  const acessoMetros = (saida.distancia + chegada.distancia) * metrosPorUnidade;
  return {
    pontos: Float32Array.from(linha),
    acessos,
    distanciaMetros: Math.round(metros + acessoMetros),
    nomes,
    segmentos: segmentos.map((item) => ({ nome: item.nome, pontos: Float32Array.from(item.pontos) })),
  };
}

/**
 * Partes da rota que passam por vias com ocorrência, na cor do pior risco de cada uma.
 * alertas: ocorrências no caminho, vindas da API ({ via, risco }).
 */
export function trechosComRisco(rota, alertas, nomesPorVia) {
  const pior = new Map();
  alertas.forEach(({ via, risco }) => {
    (nomesPorVia.get(via.id) ?? []).forEach((nome) => {
      if (!pior.has(nome) || ORDEM_RISCO[risco.nivel] > ORDEM_RISCO[pior.get(nome)]) pior.set(nome, risco.nivel);
    });
  });
  return rota.segmentos
    .filter((segmento) => pior.has(segmento.nome))
    .map((segmento) => ({ pontos: segmento.pontos, nivel: pior.get(segmento.nome) }));
}

// ------------------------------------------------- vias do banco na rota

/**
 * Para cada via cadastrada no banco, os nomes do OpenStreetMap que formam o traçado dela
 * (a "Radial Leste", por exemplo, é um corredor com vários nomes).
 */
export function nomesDasVias(dados, vias) {
  const resultado = new Map();
  vias.forEach((via) => {
    const nomes = new Set(tracadoDaVia(dados, via).map((indice) => dados.ruas[indice].nome));
    nomes.delete(-1);
    resultado.set(via.id, nomes);
  });
  return resultado;
}

/** Vias cadastradas que a rota percorre, na ordem em que aparecem no caminho. */
export function viasNaRota(rota, vias, nomesPorVia) {
  const ordem = new Map(rota.nomes.map((nome, i) => [nome, i]));
  return vias
    .map((via) => {
      const posicoes = [...(nomesPorVia.get(via.id) ?? [])].map((nome) => ordem.get(nome)).filter((p) => p !== undefined);
      return posicoes.length ? { via, posicao: Math.min(...posicoes) } : null;
    })
    .filter(Boolean)
    .sort((a, b) => a.posicao - b.posicao)
    .map((item) => item.via);
}

/**
 * Multiplicadores de custo para a rota mais segura: cada nome do OpenStreetMap que pertence a
 * uma via com ocorrência ativa recebe a penalidade do pior risco para o modal.
 */
export function penalidadesDeRisco(ocorrencias, nomesPorVia) {
  const pior = new Map();
  ocorrencias.forEach((ocorrencia) => {
    const nivel = ocorrencia.risco?.nivel;
    if (!nivel || ORDEM_RISCO[nivel] < ORDEM_RISCO.MODERADO) return;
    (nomesPorVia.get(ocorrencia.via.id) ?? []).forEach((nome) => {
      if (!pior.has(nome) || ORDEM_RISCO[nivel] > ORDEM_RISCO[pior.get(nome)]) pior.set(nome, nivel);
    });
  });
  return new Map([...pior].map(([nome, nivel]) => [nome, PENALIDADE_RISCO[nivel]]));
}

// ------------------------------------------------------------------ busca

const semAcento = (texto) =>
  (texto ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .trim();

/** Bairro mais próximo de um ponto, para dar contexto ("Rua Augusta · Consolação"). */
export function bairroPerto(dados, x, y) {
  let melhor = null;
  let menor = Infinity;
  dados.lugares.forEach((lugar) => {
    const d = Math.hypot(lugar.x - x, lugar.y - y);
    if (d < menor) {
      menor = d;
      melhor = lugar;
    }
  });
  return menor < 80 ? melhor?.texto : null;
}

let indiceBusca = null;

/** Índice de busca: bairros e ruas com nome, cada um com um ponto dentro da cidade. */
function montarIndice(dados) {
  const itens = [];
  dados.lugares.forEach((lugar) => {
    if (!zonaNoPonto(dados, lugar.x, lugar.y)) return;
    itens.push({ tipo: "bairro", nome: lugar.texto, chave: semAcento(lugar.texto), x: lugar.x, y: lugar.y });
  });
  const vistos = new Set();
  dados.ruas.forEach((rua) => {
    if (rua.nome < 0) return;
    const nome = dados.nomes[rua.nome].replace(/\s*\([^)]*\)/g, "");
    const chave = semAcento(nome);
    if (vistos.has(chave)) return;
    // Ponto da rua: o meio da linha mais comprida com esse nome dentro da cidade
    const indices = dados.porNome.get(chaveVia(dados.nomes[rua.nome])) ?? [];
    let melhor = null;
    let tamanho = -1;
    let classe = 9;
    indices.forEach((indice) => {
      const { pontos, caixa, classe: daLinha } = dados.ruas[indice];
      const meio = Math.floor(pontos.length / 4) * 2;
      const px = pontos[meio];
      const py = pontos[meio + 1];
      if (!zonaNoPonto(dados, px, py)) return;
      // A classe que vale é a mais importante (as alças de acesso, classe 7, não contam)
      if (daLinha !== 7) classe = Math.min(classe, daLinha);
      const t = caixa[2] - caixa[0] + caixa[3] - caixa[1];
      if (t > tamanho) {
        tamanho = t;
        melhor = [px, py];
      }
    });
    if (!melhor) return;
    vistos.add(chave);
    // Núcleo do nome, sem o tipo: "avenida paulista" → "paulista"
    const nucleo = chave.replace(TIPO_LOGRADOURO, "");
    itens.push({ tipo: "rua", nome, chave, nucleo, x: melhor[0], y: melhor[1], classe: classe === 9 ? 5 : classe });
  });
  return itens;
}

const TIPO_LOGRADOURO =
  /^(rua|avenida|alameda|praca|travessa|estrada|rodovia|viaduto|ponte|largo|ladeira|tunel|via|marginal|complexo viario)\s+((de|da|do|das|dos)\s+)?/;

/** Prepara o índice de busca sem travar a digitação: chame assim que o mapa carregar. */
export function prepararBusca(dados) {
  if (!indiceBusca || indiceBusca.dados !== dados) indiceBusca = { dados, itens: montarIndice(dados) };
}

/**
 * Sugestões para o texto digitado. A pontuação favorece, nesta ordem: o nome exato,
 * o nome sem o tipo ("paulista" acha a Avenida Paulista), o começo do nome e as vias
 * mais importantes (vias expressas e avenidas antes de ruas de bairro).
 */
export function buscarLugares(dados, texto, limite = 8) {
  const busca = semAcento(texto);
  if (busca.length < 2) return [];
  prepararBusca(dados);
  const palavras = busca.split(/\s+/);
  const pontuar = (item) => {
    if (!palavras.every((p) => item.chave.includes(p))) return -1;
    let pontos = 0;
    if (item.chave === busca) pontos += 60;
    else if (item.nucleo === busca) pontos += 45;
    else if (item.chave.startsWith(busca)) pontos += 32;
    else if (item.nucleo?.startsWith(busca)) pontos += 26;
    else if (item.chave.includes(` ${palavras[0]}`)) pontos += 15;
    pontos += item.tipo === "bairro" ? 8 : Math.max(0, 5 - item.classe) * 2;
    return pontos - item.chave.length / 40;
  };
  return indiceBusca.itens
    .map((item) => ({ item, pontos: pontuar(item) }))
    .filter(({ pontos }) => pontos >= 0)
    .sort((a, b) => b.pontos - a.pontos)
    .slice(0, limite)
    .map(({ item }) => ({
      ...item,
      contexto: item.tipo === "bairro" ? "Bairro" : (bairroPerto(dados, item.x, item.y) ?? "São Paulo"),
    }));
}

/** Nome para um ponto clicado no mapa: a via mais próxima ("Perto da Avenida Paulista"). */
export function descreverPonto(dados, rede, x, y) {
  const [perto] = nosProximos(rede, x, y, 1);
  if (perto) {
    for (let p = rede.inicio[perto.no]; p < rede.inicio[perto.no + 1]; p += 1) {
      const { nome } = rede.arestas[rede.aresta[p]];
      if (nome >= 0) return `Perto de ${dados.nomes[nome].replace(/\s*\([^)]*\)/g, "")}`;
    }
  }
  const bairro = bairroPerto(dados, x, y);
  return bairro ? `Ponto no mapa · ${bairro}` : "Ponto escolhido no mapa";
}

/** Minutos estimados para a distância, na velocidade média do modal. */
export function minutosPara(distanciaMetros, velocidadeKmh) {
  return Math.max(1, Math.ceil(distanciaMetros / ((velocidadeKmh * 1000) / 60)));
}

/** Corpo do PUT a partir de uma rota já salva (a API recebe a rota inteira). */
export function corpoDaRota(rota, mudancas = {}) {
  return {
    nome: rota.nome,
    distanciaMetros: rota.distanciaMetros,
    origem: rota.origem,
    destino: rota.destino,
    vias: rota.vias.map((via) => via.id),
    ...mudancas,
  };
}
