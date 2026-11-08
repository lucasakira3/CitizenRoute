// Mapa de São Paulo: carrega o arquivo gerado a partir do OpenStreetMap
// (public/mapa/sao-paulo.json, criado por scripts/gerar-mapa.mjs) e prepara os
// dados para o desenho: decodifica as linhas, calcula a caixa de cada feição
// (para desenhar só o que está na tela) e monta o índice de ruas por nome, que é
// como as vias do banco são encontradas no mapa.
//
// Plano do mapa: x cresce para o leste e y para o sul, em unidades de 25 m.

export const ZONAS = [
  { chave: "CENTRO", nome: "Centro" },
  { chave: "NORTE", nome: "Zona Norte" },
  { chave: "SUL", nome: "Zona Sul" },
  { chave: "LESTE", nome: "Zona Leste" },
  { chave: "OESTE", nome: "Zona Oeste" },
];

/** Converte o nome da região do banco na zona do mapa ("Zona Leste" → LESTE). */
export function zonaDaRegiao(nome) {
  const texto = (nome ?? "").toLowerCase();
  if (texto.includes("norte")) return "NORTE";
  if (texto.includes("sul")) return "SUL";
  if (texto.includes("leste")) return "LESTE";
  if (texto.includes("oeste")) return "OESTE";
  if (texto.includes("centro")) return "CENTRO";
  return null;
}

// Áreas que a câmera enquadra, em latitude e longitude
const AREAS = {
  cidade: { norte: -23.46, sul: -23.68, oeste: -46.77, leste: -46.42 },
  nucleo: { norte: -23.47, sul: -23.66, oeste: -46.75, leste: -46.5 },
  celular: { norte: -23.5, sul: -23.62, oeste: -46.72, leste: -46.56 },
};

// Nomes do banco que no OpenStreetMap estão escritos de outro jeito. A Radial Leste,
// por exemplo, é um corredor formado por várias avenidas com nomes próprios.
const APELIDOS = {
  "avenida 23 de maio": ["avenida vinte e tres de maio", "avenida 23 de maio"],
  "radial leste": [
    "viaduto alcantara machado",
    "avenida alcantara machado",
    "avenida conde de frontin",
    "avenida antonio estevao de carvalho",
  ],
};

const TITULOS =
  /\b(doutor|doutora|professor|professora|engenheiro|brigadeiro|presidente|governador|senador|deputado|padre|dom|general|marechal|coronel|comendador|prefeito|vereador|ministro|almirante|capitao|tenente|monsenhor|dr|prof|eng|brig|pres|gov|sen|dep|gen|cel)\b\.?/g;

/** Nome comparável: sem acento, minúsculo e com as abreviações das placas expandidas. */
export function chaveVia(nome) {
  return ` ${nome ?? ""} `
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/\s*\([^)]*\)/g, " ")
    .replace(/\sav\.?\s/g, " avenida ")
    .replace(/\sr\.?\s/g, " rua ")
    .replace(/\s+/g, " ")
    .trim();
}

/** Versão mais solta, sem títulos: "Av. Dr. Ricardo Jafet" combina com "Avenida Ricardo Jafet". */
function chaveSolta(nome) {
  return chaveVia(nome)
    .replace(TITULOS, " ")
    .replace(/\b(de|da|do|das|dos|e)\b/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

// ------------------------------------------------------------- carregamento

let promessa = null;

/** Carrega o mapa uma vez só; as outras páginas reaproveitam o mesmo resultado. */
export function carregarMapa() {
  if (!promessa) {
    promessa = fetch(`${import.meta.env.BASE_URL}mapa/sao-paulo.json`)
      .then((resposta) => {
        if (!resposta.ok) throw new Error("Não foi possível carregar o mapa da cidade.");
        return resposta.json();
      })
      .then(preparar)
      .catch((erro) => {
        promessa = null;
        throw erro;
      });
  }
  return promessa;
}

function decodificar(numeros, inicio, escala) {
  const quantidade = (numeros.length - inicio) / 2;
  const pontos = new Float32Array(quantidade * 2);
  let x = 0;
  let y = 0;
  for (let i = 0; i < quantidade; i += 1) {
    x += numeros[inicio + i * 2];
    y += numeros[inicio + i * 2 + 1];
    pontos[i * 2] = x / escala;
    pontos[i * 2 + 1] = y / escala;
  }
  return pontos;
}

function caixa(pontos) {
  let x0 = Infinity;
  let y0 = Infinity;
  let x1 = -Infinity;
  let y1 = -Infinity;
  for (let i = 0; i < pontos.length; i += 2) {
    if (pontos[i] < x0) x0 = pontos[i];
    if (pontos[i] > x1) x1 = pontos[i];
    if (pontos[i + 1] < y0) y0 = pontos[i + 1];
    if (pontos[i + 1] > y1) y1 = pontos[i + 1];
  }
  return [x0, y0, x1, y1];
}

function caixaDeVarias(listas) {
  return listas
    .map(caixa)
    .reduce((a, b) => [Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[2], b[2]), Math.max(a[3], b[3])]);
}

export function comprimento(pontos) {
  let total = 0;
  for (let i = 2; i < pontos.length; i += 2) {
    total += Math.hypot(pontos[i] - pontos[i - 2], pontos[i + 1] - pontos[i - 1]);
  }
  return total;
}

/** Ponto a uma fração (0 a 1) do comprimento de uma linha. */
export function pontoNaFracao(pontos, fracao) {
  let restante = comprimento(pontos) * Math.min(Math.max(fracao, 0), 1);
  for (let i = 2; i < pontos.length; i += 2) {
    const trecho = Math.hypot(pontos[i] - pontos[i - 2], pontos[i + 1] - pontos[i - 1]);
    if (restante <= trecho) {
      const t = trecho ? restante / trecho : 0;
      return {
        x: pontos[i - 2] + (pontos[i] - pontos[i - 2]) * t,
        y: pontos[i - 1] + (pontos[i + 1] - pontos[i - 1]) * t,
      };
    }
    restante -= trecho;
  }
  return { x: pontos[pontos.length - 2], y: pontos[pontos.length - 1] };
}

export function dentroDoAnel(px, py, pontos) {
  let dentro = false;
  for (let i = 0, j = pontos.length - 2; i < pontos.length; j = i, i += 2) {
    const xi = pontos[i];
    const yi = pontos[i + 1];
    const xj = pontos[j];
    const yj = pontos[j + 1];
    if (yi > py !== yj > py && px < ((xj - xi) * (py - yi)) / (yj - yi) + xi) dentro = !dentro;
  }
  return dentro;
}

function preparar(bruto) {
  const { escala } = bruto;
  const linha = (numeros, inicio = 0) => decodificar(numeros, inicio, escala);
  const comCaixa = (item, inicio) => {
    const pontos = linha(item, inicio);
    return { tipo: item[0], pontos, caixa: caixa(pontos) };
  };

  const ruas = bruto.ruas.map((item) => {
    const pontos = linha(item, 2);
    return { classe: item[0], nome: item[1], pontos, caixa: caixa(pontos) };
  });

  // Índice por nome: é por ele que uma via do banco ("Avenida Paulista") acha o seu traçado
  const porNome = new Map();
  const porNomeSolto = new Map();
  ruas.forEach((rua, indice) => {
    if (rua.nome < 0) return;
    const nome = bruto.nomes[rua.nome];
    [
      [porNome, chaveVia(nome)],
      [porNomeSolto, chaveSolta(nome)],
    ].forEach(([mapa, chave]) => {
      if (!mapa.has(chave)) mapa.set(chave, []);
      mapa.get(chave).push(indice);
    });
  });

  const { limites, metrosPorUnidade } = bruto;
  const metrosPorGrauLon = 111320 * Math.cos((-23.68 * Math.PI) / 180);
  const projetarLatLon = (lat, lon) => [
    ((lon - limites.oeste) * metrosPorGrauLon) / metrosPorUnidade,
    ((limites.norte - lat) * 110786) / metrosPorUnidade,
  ];
  const areas = Object.fromEntries(
    Object.entries(AREAS).map(([chave, area]) => {
      const [x0, y0] = projetarLatLon(area.norte, area.oeste);
      const [x1, y1] = projetarLatLon(area.sul, area.leste);
      return [chave, { x0, y0, x1, y1 }];
    }),
  );

  const zonas = ZONAS.map((zona, indice) => {
    const aneis = bruto.zonasAneis[indice].map((anel) => linha(anel));
    const [x0, y0, x1, y1] = caixaDeVarias(aneis);
    const [cx, cy] = bruto.centrosZonas[indice];
    return { ...zona, aneis, centro: [cx / escala, cy / escala], caixa: { x0, y0, x1, y1 } };
  });

  return {
    fonte: bruto.fonte,
    largura: bruto.largura,
    altura: bruto.altura,
    nomes: bruto.nomes,
    ruas,
    porNome,
    porNomeSolto,
    macro: bruto.macro.map((item) => {
      const pontos = linha(item, 1);
      return { classe: item[0], pontos, caixa: caixa(pontos) };
    }),
    rotulos: bruto.rotulos.map(([nome, classe, x, y, angulo], indice) => ({
      id: indice,
      // "Marginal Tietê (Expressa)" aparece como "Marginal Tietê", como nos apps de mapa
      texto: bruto.nomes[nome].replace(/\s*\([^)]*\)/g, ""),
      classe,
      x: x / escala,
      y: y / escala,
      angulo: (angulo * Math.PI) / 180,
    })),
    limite: bruto.limite.map((anel) => linha(anel)),
    zonas,
    areas,
    agua: bruto.agua.map((aneisBrutos) => {
      const aneis = aneisBrutos.map((anel) => linha(anel));
      return { aneis, caixa: caixaDeVarias(aneis) };
    }),
    rios: bruto.rios.map((item) => comCaixa(item, 1)),
    verde: bruto.verde.map((item) => {
      const aneis = item.slice(1).map((anel) => linha(anel));
      return { tipo: item[0], aneis, caixa: caixaDeVarias(aneis) };
    }),
    aeroportos: bruto.aeroportos.map(([tipo, numeros]) => {
      const pontos = linha(numeros);
      return { tipo, pontos, caixa: caixa(pontos) };
    }),
    trilhos: bruto.trilhos.map((item) => comCaixa(item, 1)),
    lugares: bruto.lugares.map(([texto, tipo, x, y], indice) => ({ id: indice, texto, tipo, x: x / escala, y: y / escala })),
    rotulosAreas: bruto.rotulosAreas.map(([texto, tipo, x, y, area], indice) => ({
      id: indice,
      texto,
      tipo,
      x: x / escala,
      y: y / escala,
      area,
    })),
    predios: bruto.predios.map((item) => prepararPredio(linha(item, 1), item[0] / escala)),
  };
}

// Luz do sol vindo do noroeste: paredes viradas para ele ficam mais claras
const SOL = { x: -0.55, y: -0.83 };

/**
 * Para cada parede do prédio: a direção para fora (normal) e a cor, conforme o sol.
 * Calculado uma vez aqui para o desenho de cada quadro só consultar.
 */
function prepararPredio(pontos, altura) {
  const [x0, y0, x1, y1] = caixa(pontos);
  const n = pontos.length / 2;
  let soma = 0;
  for (let i = 0; i < n; i += 1) {
    const j = (i + 1) % n;
    soma += pontos[i * 2] * pontos[j * 2 + 1] - pontos[j * 2] * pontos[i * 2 + 1];
  }
  const sentido = soma > 0 ? 1 : -1;
  const normais = new Float32Array(n * 2);
  const noSol = new Uint8Array(n);
  for (let i = 0; i < n; i += 1) {
    const j = (i + 1) % n;
    const ex = pontos[j * 2] - pontos[i * 2];
    const ey = pontos[j * 2 + 1] - pontos[i * 2 + 1];
    const tamanho = Math.hypot(ex, ey) || 1;
    const nx = (sentido * ey) / tamanho;
    const ny = (-sentido * ex) / tamanho;
    normais[i * 2] = nx;
    normais[i * 2 + 1] = ny;
    noSol[i] = nx * SOL.x + ny * SOL.y > 0 ? 1 : 0;
  }
  return { altura, pontos, normais, noSol, caixa: [x0, y0, x1, y1], cx: (x0 + x1) / 2, cy: (y0 + y1) / 2 };
}

/**
 * Linhas do mapa que formam uma via do banco: todas as do OpenStreetMap com o mesmo
 * nome (as duas pistas de uma avenida, por exemplo). Devolve [] se não encontrar.
 */
export function tracadoDaVia(dados, via) {
  if (!dados || !via) return [];
  const chave = chaveVia(via.nome);
  // Todas as pistas e trechos com o nome (ou com os nomes do corredor)
  const achados = (APELIDOS[chave] ?? [chave]).flatMap((nome) => dados.porNome.get(nome) ?? []);
  if (achados.length) return achados;
  return dados.porNomeSolto.get(chaveSolta(via.nome)) ?? [];
}

/** Zona onde fica um ponto do plano (o clique no mapa filtra por zona). */
export function zonaNoPonto(dados, x, y) {
  const zona = dados.zonas.find((z) => z.aneis.some((anel) => dentroDoAnel(x, y, anel)));
  return zona?.chave ?? null;
}
