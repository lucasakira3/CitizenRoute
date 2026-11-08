import { useCallback, useEffect, useState } from "react";
import { useApp } from "../contexto.js";
import { api } from "../services/api.js";

/** Itens do menu principal. É o mesmo menu em todas as páginas. */
const ITENS_MENU = [
  { caminho: "/", rotulo: "Painel" },
  { caminho: "/mapa", rotulo: "Mapa" },
  { caminho: "/ocorrencias", rotulo: "Ocorrências" },
  { caminho: "/meu-trajeto", rotulo: "Meu trajeto" },
  { caminho: "/gestao", rotulo: "Gestão" },
  { caminho: "/relatorios", rotulo: "Relatórios" },
  { caminho: "/cadastros", rotulo: "Cadastros" },
];

function estaAtivo(caminho, rota) {
  if (caminho === "/") {
    return rota === "/";
  }
  return rota === caminho || rota.startsWith(`${caminho}/`);
}

// Páginas que ocupam a tela inteira abaixo do cabeçalho (sem rodapé e sem margens)
const TELA_CHEIA = new Set(["/mapa"]);

export default function Layout({ rota, children }) {
  const { sessao, sair } = useApp();
  const [menuAberto, setMenuAberto] = useState(false);
  const telaCheia = TELA_CHEIA.has(rota);

  useEffect(() => {
    setMenuAberto(false);
  }, [rota]);

  function pularParaConteudo(evento) {
    evento.preventDefault();
    document.getElementById("conteudo")?.focus();
  }

  return (
    <>
      <a className="pular" href="#conteudo" onClick={pularParaConteudo}>
        Pular para o conteúdo
      </a>

      <header className="cabecalho">
        <div className="cabecalho__interno">
          <a className="marca" href="#/" aria-label="CitizenRoute, ir para o painel">
            <span className="marca__sinal" aria-hidden="true" />
            <span className="marca__nome">CitizenRoute</span>
          </a>

          <button
            type="button"
            className="menu-botao"
            aria-expanded={menuAberto}
            aria-controls="menu-principal"
            onClick={() => setMenuAberto((aberto) => !aberto)}
          >
            {menuAberto ? "Fechar" : "Menu"}
          </button>

          <nav id="menu-principal" className={`menu ${menuAberto ? "menu--aberto" : ""}`} aria-label="Principal">
            <ul className="menu__lista">
              {ITENS_MENU.map((item) => (
                <li key={item.caminho}>
                  <a
                    className="menu__link"
                    href={`#${item.caminho}`}
                    aria-current={estaAtivo(item.caminho, rota) ? "page" : undefined}
                  >
                    {item.rotulo}
                  </a>
                </li>
              ))}
            </ul>

            <div className="sessao">
              {sessao ? (
                <>
                  <span className="sessao__quem">
                    <span className="sessao__nome">{sessao.nome.split(" ")[0]}</span>
                    <span className="sessao__perfil">{sessao.perfil}</span>
                  </span>
                  <button type="button" className="sessao__sair" onClick={sair}>
                    Sair
                  </button>
                </>
              ) : (
                <a
                  className="botao botao--sinal"
                  href="#/entrar"
                  aria-current={rota === "/entrar" ? "page" : undefined}
                >
                  Entrar
                </a>
              )}
            </div>
          </nav>
        </div>
      </header>

      <FaixaStatus />

      <main id="conteudo" className={`conteudo ${telaCheia ? "conteudo--tela-cheia" : ""}`} tabIndex={-1}>
        {children}
      </main>

      {!telaCheia && (
        <footer className="rodape">
          <div className="rodape__interno">
            <p>
              <strong>CitizenRoute</strong> · o cidadão se protege, a gestão age.
            </p>
            <p>Global Solution 2026/2 · CitizenTech · Engenharia de Software FIAP</p>
          </div>
        </footer>
      )}
    </>
  );
}

/** Faixa zebrada, como fita de obra, que só aparece quando a API ou o banco estão fora do ar. */
function FaixaStatus() {
  const [saude, setSaude] = useState(null);
  const [erro, setErro] = useState(null);

  const verificar = useCallback(() => {
    api
      .buscar("/health")
      .then((resposta) => {
        setSaude(resposta);
        setErro(null);
      })
      .catch(setErro);
  }, []);

  useEffect(() => {
    verificar();
  }, [verificar]);

  const bancoFora = saude && !saude.banco.conectado;
  if (!erro && !bancoFora) {
    return null;
  }

  return (
    <div className="faixa-status" role="alert">
      <div className="faixa-status__interno">
        <p>
          <strong>{erro ? "API fora do ar." : "Banco indisponível."}</strong> {erro ? erro.message : saude.banco.mensagem}
        </p>
        <button type="button" className="botao botao--pequeno botao--secundario" onClick={verificar}>
          Verificar de novo
        </button>
      </div>
    </div>
  );
}
