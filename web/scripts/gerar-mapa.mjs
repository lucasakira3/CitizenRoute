// Gera public/mapa/sao-paulo.json, o mapa de São Paulo usado pela página Mapa.
//
// Uso (precisa de internet só nesta hora):   node scripts/gerar-mapa.mjs
//
// Os dados vêm do OpenStreetMap (© colaboradores do OpenStreetMap, licença ODbL),
// pela Overpass API. O script baixa uma única vez, guarda as respostas numa pasta
// de cache e transforma tudo num arquivo compacto: coordenadas já projetadas no
// plano do mapa (1 unidade = 25 m), linhas simplificadas e ruas com o mesmo nome
// emendadas. O site não acessa nenhum serviço externo: ele lê só esse arquivo.
//
// Não é preciso rodar de novo para usar o sistema; o arquivo gerado já está no projeto.

import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const AQUI = dirname(fileURLToPath(import.meta.url));
const SAIDA = join(AQUI, "..", "public", "mapa", "sao-paulo.json");
const CACHE = process.env.CACHE_MAPA ?? join(tmpdir(), "citizenroute-osm");
// Servidores públicos da Overpass API: se o primeiro estiver ocupado, tenta o segundo
const SERVIDORES = ["https://overpass-api.de/api/interpreter", "https://overpass.kumi.systems/api/interpreter"];
const AREA_SP = "area(3600298285)->.sp;"; // relação 298285 = município de São Paulo

// Recorte do mapa: o município inteiro e um pouco dos vizinhos
const LIMITES = { norte: -23.3, sul: -24.07, oeste: -46.89, leste: -46.3 };
const BBOX = `(${LIMITES.sul},${LIMITES.oeste},${LIMITES.norte},${LIMITES.leste})`;

const CONSULTAS = {
  limite: "rel(298285);out geom;",
  distritos: `${AREA_SP}rel(area.sp)["boundary"="administrative"]["admin_level"="9"];out geom;`,
  ruas: `${AREA_SP}way(area.sp)["highway"~"^(motorway|motorway_link|trunk|trunk_link|primary|primary_link|secondary|secondary_link|tertiary|tertiary_link)$"];out geom;`,
  locais: `${AREA_SP}way(area.sp)["highway"~"^(residential|unclassified|living_street|pedestrian)$"];out geom;`,
  entorno: `way["highway"~"^(motorway|trunk|primary|secondary)$"]${BBOX};out geom;`,
  agua: `${AREA_SP}(way(area.sp)["natural"="water"];rel(area.sp)["natural"="water"];way(area.sp)["waterway"~"^(river|canal)$"];);out geom;`,
  parques: `${AREA_SP}(way(area.sp)["leisure"~"^(park|nature_reserve)$"];rel(area.sp)["leisure"~"^(park|nature_reserve)$"];);out geom;`,
  lugares: `${AREA_SP}node(area.sp)["place"~"^(suburb|neighbourhood|quarter)$"];out;`,
  predios: `${AREA_SP}(way(area.sp)["building"]["height"](if:number(t["height"])>=35);way(area.sp)["building"]["building:levels"](if:number(t["building:levels"])>=12););out geom;`,
  trilhos: `${AREA_SP}way(area.sp)["railway"~"^(rail|light_rail|monorail|subway)$"];out geom;`,
  aeroportos: `${AREA_SP}way(area.sp)["aeroway"~"^(aerodrome|runway)$"];out geom;`,
  // As matas da Cantareira e da Serra do Mar são enormes: a geometria vem recortada no limite do mapa
  matas: `${AREA_SP}(way(area.sp)["landuse"="forest"];way(area.sp)["natural"="wood"];rel(area.sp)["landuse"="forest"];rel(area.sp)["natural"="wood"];);out geom${BBOX};`,
  gramados: `${AREA_SP}way(area.sp)["landuse"~"^(grass|cemetery)$"];out geom;`,
};

async function baixar(nome) {
  mkdirSync(CACHE, { recursive: true });
  const arquivo = join(CACHE, `${nome}.json`);
  if (existsSync(arquivo)) {
    return JSON.parse(readFileSync(arquivo, "utf8"));
  }
  // --offline: usa só o que já está no cache (útil para testar o desenho sem esperar o servidor)
  if (process.argv.includes("--offline")) {
    console.log(`  ${nome}: fora do cache, ficou vazio`);
    return { elements: [] };
  }
  // Sem pedir memória extra: com o servidor cheio, consultas que pedem muita memória são recusadas
  const consulta = `[out:json][timeout:240];${CONSULTAS[nome]}`;
  for (let tentativa = 1; tentativa <= 6; tentativa += 1) {
    const servidor = SERVIDORES[(tentativa - 1) % SERVIDORES.length];
    console.log(`baixando ${nome} (tentativa ${tentativa}, ${new URL(servidor).host})...`);
    const resposta = await fetch(servidor, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "User-Agent": "CitizenRoute-gerador-de-mapa/1.0 (trabalho academico FIAP)",
      },
      body: new URLSearchParams({ data: consulta }),
    });
    if (resposta.ok) {
      const texto = await resposta.text();
      writeFileSync(arquivo, texto);
      return JSON.parse(texto);
    }
    console.log(`  resposta ${resposta.status}; esperando para tentar de novo`);
    await new Promise((ok) => setTimeout(ok, 10000 * tentativa));
  }
  throw new Error(`Não foi possível baixar ${nome}`);
}

export async function baixarTudo() {
  const dados = {};
  for (const nome of Object.keys(CONSULTAS)) {
    dados[nome] = await baixar(nome);
    console.log(`  ${nome}: ${dados[nome].elements.length} elementos`);
  }
  return dados;
}

// ------------------------------------------------------------------ projeção

const METROS_POR_UNIDADE = 25;
const ESCALA = 8; // coordenadas gravadas como inteiros: unidade × 8 (≈ 3 m de precisão)
const LAT_REFERENCIA = -23.68;
const M_POR_GRAU_LAT = 110786;
const M_POR_GRAU_LON = 111320 * Math.cos((LAT_REFERENCIA * Math.PI) / 180);
const LARGURA = Math.round(((LIMITES.leste - LIMITES.oeste) * M_POR_GRAU_LON) / METROS_POR_UNIDADE);
const ALTURA = Math.round(((LIMITES.norte - LIMITES.sul) * M_POR_GRAU_LAT) / METROS_POR_UNIDADE);

/** Latitude e longitude → ponto do plano (x para leste, y para o sul), em unidades de 25 m. */
function projetar({ lat, lon }) {
  return [
    ((lon - LIMITES.oeste) * M_POR_GRAU_LON) / METROS_POR_UNIDADE,
    ((LIMITES.norte - lat) * M_POR_GRAU_LAT) / METROS_POR_UNIDADE,
  ];
}

// --------------------------------------------------------------- geometria

/** Douglas-Peucker: tira pontos que quase não mudam o desenho (tolerância em unidades). */
function simplificar(pontos, tolerancia) {
  if (pontos.length <= 2) return pontos;
  const manter = new Uint8Array(pontos.length);
  manter[0] = 1;
  manter[pontos.length - 1] = 1;
  const pilha = [[0, pontos.length - 1]];
  while (pilha.length) {
    const [a, b] = pilha.pop();
    const [ax, ay] = pontos[a];
    const [bx, by] = pontos[b];
    const dx = bx - ax;
    const dy = by - ay;
    const comprimento = Math.hypot(dx, dy);
    let maior = -1;
    let indice = -1;
    for (let i = a + 1; i < b; i += 1) {
      // Num anel fechado o início e o fim são o mesmo ponto: aí a medida é a distância até ele
      const distancia =
        comprimento < 1e-9
          ? Math.hypot(pontos[i][0] - ax, pontos[i][1] - ay)
          : Math.abs(dy * pontos[i][0] - dx * pontos[i][1] + bx * ay - by * ax) / comprimento;
      if (distancia > maior) {
        maior = distancia;
        indice = i;
      }
    }
    if (maior > tolerancia) {
      manter[indice] = 1;
      pilha.push([a, indice], [indice, b]);
    }
  }
  return pontos.filter((_, i) => manter[i]);
}

/** Grava uma linha como [x0, y0, dx1, dy1, ...] em inteiros: o arquivo fica bem menor. */
function codificar(pontos) {
  const saida = [];
  let anteriorX = 0;
  let anteriorY = 0;
  pontos.forEach(([x, y], i) => {
    const qx = Math.round(x * ESCALA);
    const qy = Math.round(y * ESCALA);
    if (i > 0 && qx === anteriorX && qy === anteriorY) return;
    if (i === 0) saida.push(qx, qy);
    else saida.push(qx - anteriorX, qy - anteriorY);
    anteriorX = qx;
    anteriorY = qy;
  });
  return saida;
}

function areaDoAnel(pontos) {
  let soma = 0;
  for (let i = 0, j = pontos.length - 1; i < pontos.length; j = i, i += 1) {
    soma += (pontos[j][0] + pontos[i][0]) * (pontos[j][1] - pontos[i][1]);
  }
  return Math.abs(soma / 2);
}

function centroDoAnel(pontos) {
  let area = 0;
  let cx = 0;
  let cy = 0;
  for (let i = 0, j = pontos.length - 1; i < pontos.length; j = i, i += 1) {
    const f = pontos[j][0] * pontos[i][1] - pontos[i][0] * pontos[j][1];
    area += f;
    cx += (pontos[j][0] + pontos[i][0]) * f;
    cy += (pontos[j][1] + pontos[i][1]) * f;
  }
  if (Math.abs(area) < 1e-9) return pontos[0];
  return [cx / (3 * area), cy / (3 * area)];
}

function comprimentoDe(pontos) {
  let total = 0;
  for (let i = 1; i < pontos.length; i += 1) {
    total += Math.hypot(pontos[i][0] - pontos[i - 1][0], pontos[i][1] - pontos[i - 1][1]);
  }
  return total;
}

/**
 * Emenda pedaços de linha que se tocam nas pontas. Serve para montar os anéis das
 * áreas (represas, distritos) e para juntar os muitos trechos de uma mesma avenida.
 * Cada pedaço é { chaves: [inicio, fim], pontos }.
 */
function emendar(pedacos) {
  const porPonta = new Map();
  pedacos.forEach((pedaco, indice) => {
    pedaco.chaves.forEach((chave) => {
      if (!porPonta.has(chave)) porPonta.set(chave, []);
      porPonta.get(chave).push(indice);
    });
  });
  const usado = new Uint8Array(pedacos.length);
  const cadeias = [];
  const proximo = (chave) => (porPonta.get(chave) ?? []).find((i) => !usado[i]);

  pedacos.forEach((pedaco, indice) => {
    if (usado[indice]) return;
    usado[indice] = 1;
    let pontos = [...pedaco.pontos];
    let [inicio, fim] = pedaco.chaves;
    // Para a frente
    for (let i = proximo(fim); i !== undefined; i = proximo(fim)) {
      usado[i] = 1;
      const outro = pedacos[i];
      if (outro.chaves[0] === fim) {
        pontos = pontos.concat(outro.pontos.slice(1));
        fim = outro.chaves[1];
      } else {
        pontos = pontos.concat([...outro.pontos].reverse().slice(1));
        fim = outro.chaves[0];
      }
      if (fim === inicio) break;
    }
    // Para trás
    for (let i = fim === inicio ? undefined : proximo(inicio); i !== undefined; i = proximo(inicio)) {
      usado[i] = 1;
      const outro = pedacos[i];
      if (outro.chaves[1] === inicio) {
        pontos = outro.pontos.slice(0, -1).concat(pontos);
        inicio = outro.chaves[0];
      } else {
        pontos = [...outro.pontos].reverse().slice(0, -1).concat(pontos);
        inicio = outro.chaves[1];
      }
    }
    cadeias.push({ pontos, fechada: inicio === fim });
  });
  return cadeias;
}

const chaveCoordenada = ({ lat, lon }) => `${lat.toFixed(7)},${lon.toFixed(7)}`;

/** Pontos de uma geometria do Overpass. Com recorte (out geom(...)), quem fica fora vem null. */
const valida = (geometria) => (geometria ?? []).filter(Boolean);

/** Anéis externos (e internos) de uma relação multipolígono ou de um caminho fechado. */
function aneisDe(elemento) {
  if (elemento.type === "way") {
    const pontos = valida(elemento.geometry);
    if (pontos.length < 4) return { externos: [], internos: [] };
    return { externos: [pontos.map(projetar)], internos: [] };
  }
  const montar = (papel) =>
    emendar(
      (elemento.members ?? [])
        .filter((m) => m.type === "way" && (m.role || "outer") === papel && valida(m.geometry).length >= 2)
        .map((m) => {
          const pontos = valida(m.geometry);
          return { chaves: [chaveCoordenada(pontos[0]), chaveCoordenada(pontos[pontos.length - 1])], pontos };
        }),
    )
      .filter((cadeia) => cadeia.pontos.length >= 4)
      .map((cadeia) => cadeia.pontos.map(projetar));
  return { externos: montar("outer"), internos: montar("inner") };
}

// ------------------------------------------------------------ classificação

const CLASSES = ["expressa", "arterial", "principal", "secundaria", "terciaria", "local", "pedestre", "acesso"];
const CLASSE_DO_OSM = {
  motorway: 0,
  trunk: 1,
  primary: 2,
  secondary: 3,
  tertiary: 4,
  residential: 5,
  unclassified: 5,
  living_street: 5,
  pedestrian: 6,
  motorway_link: 7,
  trunk_link: 7,
  primary_link: 7,
  secondary_link: 7,
  tertiary_link: 7,
};
const TOLERANCIA = [0.3, 0.3, 0.3, 0.35, 0.4, 0.5, 0.5, 0.3];
const ESPACO_ROTULO = [260, 240, 200, 160, 120, 80, 80, 0];
const MINIMO_ROTULO = [60, 50, 40, 30, 22, 14, 14, Infinity];

// Divisão oficial da Prefeitura: cada um dos 96 distritos pertence a uma subprefeitura,
// e cada subprefeitura a uma das cinco regiões usadas no banco (CR_REGIAO).
const ZONA_DO_DISTRITO = {
  CENTRO: ["Bela Vista", "Bom Retiro", "Cambuci", "Consolação", "Liberdade", "República", "Santa Cecília", "Sé"],
  OESTE: [
    "Butantã", "Morumbi", "Raposo Tavares", "Rio Pequeno", "Vila Sônia", "Barra Funda", "Jaguara", "Jaguaré", "Lapa",
    "Perdizes", "Vila Leopoldina", "Alto de Pinheiros", "Itaim Bibi", "Jardim Paulista", "Pinheiros",
  ],
  NORTE: [
    "Anhanguera", "Perus", "Jaraguá", "Pirituba", "São Domingos", "Brasilândia", "Freguesia do Ó", "Cachoeirinha",
    "Casa Verde", "Limão", "Mandaqui", "Santana", "Tucuruvi", "Jaçanã", "Tremembé", "Vila Guilherme", "Vila Maria",
    "Vila Medeiros",
  ],
  LESTE: [
    "Água Rasa", "Belém", "Brás", "Mooca", "Pari", "Tatuapé", "Artur Alvim", "Cangaíba", "Penha", "Vila Matilde",
    "Ermelino Matarazzo", "Ponte Rasa", "Jardim Helena", "São Miguel", "Vila Jacuí", "Itaim Paulista", "Vila Curuçá",
    "Guaianases", "Lajeado", "Cidade Líder", "Itaquera", "José Bonifácio", "Parque do Carmo", "Cidade Tiradentes",
    "Iguatemi", "São Mateus", "São Rafael", "São Lucas", "Vila Prudente", "Sapopemba", "Aricanduva", "Carrão",
    "Vila Formosa",
  ],
  SUL: [
    "Moema", "Saúde", "Vila Mariana", "Cursino", "Ipiranga", "Sacomã", "Jabaquara", "Cidade Ademar", "Pedreira",
    "Campo Belo", "Campo Grande", "Santo Amaro", "Campo Limpo", "Capão Redondo", "Vila Andrade", "Jardim Ângela",
    "Jardim São Luís", "Cidade Dutra", "Grajaú", "Socorro", "Marsilac", "Parelheiros",
  ],
};
const ZONAS = ["CENTRO", "NORTE", "SUL", "LESTE", "OESTE"];
const semAcento = (texto) =>
  texto
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
const ZONA_POR_NOME = new Map(
  Object.entries(ZONA_DO_DISTRITO).flatMap(([zona, nomes]) => nomes.map((nome) => [semAcento(nome), zona])),
);

// ------------------------------------------------------------------ geração

async function gerar() {
  const osm = await baixarTudo();
  const saida = {
    fonte: "© colaboradores do OpenStreetMap (ODbL)",
    gerado: new Date().toISOString().slice(0, 10),
    metrosPorUnidade: METROS_POR_UNIDADE,
    escala: ESCALA,
    largura: LARGURA,
    altura: ALTURA,
    limites: LIMITES,
    classes: CLASSES,
    zonas: ZONAS,
  };

  // Contorno do município
  const limite = aneisDe(osm.limite.elements[0]);
  saida.limite = limite.externos.map((anel) => codificar(simplificar(anel, 0.5)));

  // Zonas: juntam os distritos de cada região. A divisa entre dois distritos da mesma
  // zona aparece nos dois; a borda da zona é formada pelos trechos que aparecem uma vez só.
  const centrosZona = Object.fromEntries(ZONAS.map((zona) => [zona, { area: 0, x: 0, y: 0 }]));
  const trechosZona = Object.fromEntries(ZONAS.map((zona) => [zona, new Map()]));
  const semZona = [];
  osm.distritos.elements.forEach((elemento) => {
    const nome = (elemento.tags?.name ?? "").replace(/^Distrito d[eao]s?\s+/i, "");
    const zona = ZONA_POR_NOME.get(semAcento(nome));
    if (!zona) {
      semZona.push(nome);
      return;
    }
    (elemento.members ?? [])
      .filter((m) => m.type === "way" && m.geometry && m.role !== "inner")
      .forEach((m) => {
        const contagem = trechosZona[zona].get(m.ref);
        trechosZona[zona].set(m.ref, contagem ? { ...contagem, vezes: contagem.vezes + 1 } : { vezes: 1, membro: m });
      });
    const { externos } = aneisDe(elemento);
    externos.forEach((anel) => {
      const area = areaDoAnel(anel);
      const [cx, cy] = centroDoAnel(anel);
      const acumulado = centrosZona[zona];
      acumulado.area += area;
      acumulado.x += cx * area;
      acumulado.y += cy * area;
    });
  });
  if (semZona.length) console.log("distritos sem zona:", semZona);
  saida.zonasAneis = ZONAS.map((zona) =>
    emendar(
      [...trechosZona[zona].values()]
        .filter(({ vezes }) => vezes === 1)
        .map(({ membro }) => ({
          chaves: [chaveCoordenada(membro.geometry[0]), chaveCoordenada(membro.geometry[membro.geometry.length - 1])],
          pontos: membro.geometry,
        })),
    )
      .filter((cadeia) => cadeia.pontos.length >= 4)
      .map((cadeia) => codificar(simplificar(cadeia.pontos.map(projetar), 0.5))),
  );
  saida.centrosZonas = ZONAS.map((zona) => {
    const { area, x, y } = centrosZona[zona];
    return [Math.round((x / area) * ESCALA), Math.round((y / area) * ESCALA)];
  });

  // Ruas: trechos com o mesmo nome e a mesma classe são emendados numa linha só
  const vistos = new Set();
  const grupos = new Map();
  [...osm.ruas.elements, ...osm.locais.elements, ...osm.entorno.elements].forEach((via) => {
    if (via.type !== "way" || vistos.has(via.id) || !via.geometry || via.geometry.length < 2) return;
    vistos.add(via.id);
    const classe = CLASSE_DO_OSM[via.tags?.highway];
    if (classe === undefined) return;
    if (via.tags?.tunnel === "yes" && classe >= 5) return;
    const nome = via.tags?.name ?? "";
    const chave = `${classe}|${nome}`;
    if (!grupos.has(chave)) grupos.set(chave, []);
    grupos.get(chave).push({
      chaves: [String(via.nodes[0]), String(via.nodes[via.nodes.length - 1])],
      pontos: via.geometry.map(projetar),
    });
  });

  const nomes = [];
  const indiceNome = new Map();
  const nomeId = (nome) => {
    if (!nome) return -1;
    if (!indiceNome.has(nome)) {
      indiceNome.set(nome, nomes.length);
      nomes.push(nome);
    }
    return indiceNome.get(nome);
  };

  saida.ruas = [];
  saida.macro = [];
  saida.rotulos = [];
  grupos.forEach((pedacos, chave) => {
    const [classeTexto, nome] = chave.split(/\|(.*)/s);
    const classe = Number(classeTexto);
    const id = nomeId(nome);
    emendar(pedacos).forEach(({ pontos }) => {
      const simples = simplificar(pontos, TOLERANCIA[classe]);
      if (simples.length < 2) return;
      saida.ruas.push([classe, id, ...codificar(simples)]);
      // Versão bem leve das vias grandes, para desenhar a cidade inteira de longe
      if (classe <= 3) {
        const leve = simplificar(pontos, 2.5);
        if (comprimentoDe(leve) > 12) saida.macro.push([classe, ...codificar(leve)]);
      }
      // Onde escrever o nome: no meio da linha e, nas longas, a cada trecho
      const comprimento = comprimentoDe(simples);
      if (id < 0 || comprimento < MINIMO_ROTULO[classe]) return;
      const quantos = Math.max(1, Math.floor(comprimento / ESPACO_ROTULO[classe]));
      for (let k = 0; k < quantos; k += 1) {
        const alvo = (comprimento * (k + 0.5)) / quantos;
        const antes = pontoNoComprimento(simples, alvo - 6);
        const depois = pontoNoComprimento(simples, alvo + 6);
        const meio = pontoNoComprimento(simples, alvo);
        const angulo = (Math.atan2(depois[1] - antes[1], depois[0] - antes[0]) * 180) / Math.PI;
        saida.rotulos.push([id, classe, Math.round(meio[0] * ESCALA), Math.round(meio[1] * ESCALA), Math.round(angulo)]);
      }
    });
  });
  saida.nomes = nomes;

  // Água: rios largos, lagos e represas viram áreas; rios estreitos, linhas
  const rotulosAreas = [];
  const vistosAgua = new Set();
  saida.agua = [];
  osm.agua.elements.forEach((elemento) => {
    if (vistosAgua.has(`${elemento.type}${elemento.id}`)) return;
    vistosAgua.add(`${elemento.type}${elemento.id}`);
    if (elemento.tags?.waterway) return;
    const { externos, internos } = aneisDe(elemento);
    const area = externos.reduce((soma, anel) => soma + areaDoAnel(anel), 0);
    if (area < 25) return;
    saida.agua.push([...externos, ...internos].map((anel) => codificar(simplificar(anel, 0.4))));
    const nome = elemento.tags?.name;
    if (nome && area > 250) rotulosAreas.push([nome, 0, ...centroArredondado(externos), Math.round(area)]);
  });
  saida.rios = osm.agua.elements
    .filter((elemento) => elemento.type === "way" && elemento.tags?.waterway && elemento.geometry)
    .map((elemento) => [
      elemento.tags.waterway === "river" ? 1 : 0,
      ...codificar(simplificar(elemento.geometry.map(projetar), 0.5)),
    ]);

  // Verde: parques, matas, gramados e cemitérios
  saida.verde = [];
  [...osm.parques.elements, ...osm.matas.elements, ...osm.gramados.elements].forEach((elemento) => {
    const tags = elemento.tags ?? {};
    const tipo = tags.leisure ? 0 : tags.landuse === "grass" || tags.landuse === "cemetery" ? 2 : 1;
    const { externos, internos } = aneisDe(elemento);
    const area = externos.reduce((soma, anel) => soma + areaDoAnel(anel), 0);
    if (area < 40) return;
    saida.verde.push([tipo, ...[...externos, ...internos].map((anel) => codificar(simplificar(anel, 0.5)))]);
    if (tags.name && area > 120 && tipo !== 2) rotulosAreas.push([tags.name, 1, ...centroArredondado(externos), Math.round(area)]);
  });

  // Aeroportos (área cinza e pistas)
  saida.aeroportos = [];
  osm.aeroportos.elements.forEach((elemento) => {
    if (!elemento.geometry) return;
    const pontos = elemento.geometry.map(projetar);
    if (elemento.tags?.aeroway === "aerodrome") {
      saida.aeroportos.push([0, codificar(simplificar(pontos, 0.4))]);
      if (elemento.tags?.name) rotulosAreas.push([elemento.tags.name, 2, ...centroArredondado([pontos]), Math.round(areaDoAnel(pontos))]);
    } else {
      saida.aeroportos.push([1, codificar(simplificar(pontos, 0.2))]);
    }
  });
  saida.rotulosAreas = rotulosAreas.sort((a, b) => b[4] - a[4]);

  // Trilhos de trem e metrô na superfície
  saida.trilhos = osm.trilhos.elements
    .filter((elemento) => elemento.geometry && elemento.tags?.tunnel !== "yes" && !elemento.tags?.service)
    .map((elemento) => [
      elemento.tags.railway === "rail" ? 0 : 1,
      ...codificar(simplificar(elemento.geometry.map(projetar), 0.4)),
    ]);

  // Bairros (nomes)
  const TIPO_LUGAR = { suburb: 0, quarter: 1, neighbourhood: 2 };
  saida.lugares = osm.lugares.elements
    .filter((no) => no.tags?.name)
    .map((no) => {
      const [x, y] = projetar(no);
      return [no.tags.name, TIPO_LUGAR[no.tags.place] ?? 2, Math.round(x * ESCALA), Math.round(y * ESCALA)];
    });

  // Prédios altos com altura informada (a Prefeitura importou as alturas no OpenStreetMap)
  saida.predios = [];
  osm.predios.elements.forEach((elemento) => {
    if (!elemento.geometry || elemento.geometry.length < 4) return;
    const tags = elemento.tags ?? {};
    const metros = Number.parseFloat(tags.height) || Number.parseFloat(tags["building:levels"]) * 3.2;
    if (!Number.isFinite(metros) || metros < 35 || metros > 400) return;
    const anel = simplificar(elemento.geometry.map(projetar).slice(0, -1), 0.08);
    if (anel.length < 3 || areaDoAnel(anel) < 0.35) return;
    saida.predios.push([Math.round((metros / METROS_POR_UNIDADE) * ESCALA), ...codificar(anel)]);
  });

  mkdirSync(dirname(SAIDA), { recursive: true });
  const texto = JSON.stringify(saida);
  writeFileSync(SAIDA, texto);
  console.log(`gravado ${SAIDA}: ${(texto.length / 1024 / 1024).toFixed(2)} MB`);
  console.log(
    Object.entries(saida)
      .filter(([, valor]) => Array.isArray(valor))
      .map(([chave, valor]) => `${chave}=${valor.length}`)
      .join(" "),
  );
}

function pontoNoComprimento(pontos, alvo) {
  let restante = Math.max(0, alvo);
  for (let i = 1; i < pontos.length; i += 1) {
    const trecho = Math.hypot(pontos[i][0] - pontos[i - 1][0], pontos[i][1] - pontos[i - 1][1]);
    if (restante <= trecho) {
      const t = trecho ? restante / trecho : 0;
      return [pontos[i - 1][0] + (pontos[i][0] - pontos[i - 1][0]) * t, pontos[i - 1][1] + (pontos[i][1] - pontos[i - 1][1]) * t];
    }
    restante -= trecho;
  }
  return pontos[pontos.length - 1];
}

function centroArredondado(aneis) {
  const maior = aneis.reduce((a, b) => (areaDoAnel(b) > areaDoAnel(a) ? b : a), aneis[0]);
  const [x, y] = centroDoAnel(maior);
  return [Math.round(x * ESCALA), Math.round(y * ESCALA)];
}

if (process.argv[2] === "--so-baixar") {
  await baixarTudo();
} else {
  await gerar();
}
