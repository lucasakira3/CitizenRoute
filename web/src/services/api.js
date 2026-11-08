// Comunicação HTTP com a API Java. Toda chamada passa por aqui.
// Em desenvolvimento, o Vite repassa /api para http://127.0.0.1:8080 (veja vite.config.js).

const BASE = "/api";
const CHAVE_SESSAO = "citizenroute.sessao";

const SEM_CONEXAO =
  "Não foi possível falar com a API. Confira se o servidor Java está rodando (mvn compile exec:java, na pasta backend).";

/** Erro devolvido pela API, com o status HTTP e os erros por campo (quando houver). */
export class ErroApi extends Error {
  constructor(mensagem, status = 0, campos = {}) {
    super(mensagem);
    this.name = "ErroApi";
    this.status = status;
    this.campos = campos;
  }
}

let aoSessaoInvalida = () => {};

/** O App registra aqui o que fazer quando a API disser que a sessão expirou (HTTP 401). */
export function definirAoSessaoInvalida(funcao) {
  aoSessaoInvalida = funcao;
}

export function lerSessao() {
  try {
    const texto = localStorage.getItem(CHAVE_SESSAO);
    const sessao = texto ? JSON.parse(texto) : null;
    // Um valor estragado no navegador não pode derrubar a página: sem id e nome, não há sessão.
    return sessao && typeof sessao === "object" && sessao.id && sessao.nome ? sessao : null;
  } catch {
    return null;
  }
}

export function salvarSessao(usuario) {
  try {
    if (usuario) {
      localStorage.setItem(CHAVE_SESSAO, JSON.stringify(usuario));
    } else {
      localStorage.removeItem(CHAVE_SESSAO);
    }
  } catch {
    // Navegação privada pode bloquear o localStorage; a sessão vale só até recarregar.
  }
}

async function requisitar(metodo, caminho, corpo) {
  const sessao = lerSessao();
  const cabecalhos = { Accept: "application/json" };
  if (sessao) {
    cabecalhos["X-Usuario-Id"] = String(sessao.id);
  }
  if (corpo !== undefined) {
    cabecalhos["Content-Type"] = "application/json";
  }

  let resposta;
  try {
    resposta = await fetch(BASE + caminho, {
      method: metodo,
      headers: cabecalhos,
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    });
  } catch {
    throw new ErroApi(SEM_CONEXAO);
  }

  if (resposta.status === 204) {
    return null;
  }

  const texto = await resposta.text();
  let dados = null;
  if (texto) {
    try {
      dados = JSON.parse(texto);
    } catch {
      dados = null;
    }
  }

  if (!resposta.ok) {
    // Sem JSON de erro, quem respondeu foi o proxy do Vite: a API Java está fora do ar.
    if (!dados) {
      throw new ErroApi(SEM_CONEXAO, resposta.status);
    }
    if (resposta.status === 401 && sessao) {
      aoSessaoInvalida();
    }
    throw new ErroApi(dados.erro ?? `A API respondeu com erro ${resposta.status}.`, resposta.status, dados.campos ?? {});
  }
  return dados;
}

/** Monta "?chave=valor&..." ignorando valores vazios. */
export function consulta(parametros) {
  const busca = new URLSearchParams();
  Object.entries(parametros).forEach(([chave, valor]) => {
    if (valor !== undefined && valor !== null && valor !== "" && valor !== false) {
      busca.set(chave, String(valor));
    }
  });
  const texto = busca.toString();
  return texto ? `?${texto}` : "";
}

export const api = {
  buscar: (caminho) => requisitar("GET", caminho),
  criar: (caminho, corpo) => requisitar("POST", caminho, corpo),
  atualizar: (caminho, corpo) => requisitar("PUT", caminho, corpo),
  alterar: (caminho, corpo) => requisitar("PATCH", caminho, corpo),
  excluir: (caminho) => requisitar("DELETE", caminho),
};
