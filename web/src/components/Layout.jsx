import { useEffect, useState } from "react";
import { useApp } from "../contexto.js";
import { PILARES } from "../utils/pilares.js";
import Logo from "./Logo.jsx";

/**
 * Menu principal: os seis pilares do hub, o mesmo em todas as páginas.
 * O início fica na logo, e o perfil (rotas, endereços, meio de transporte) no bloco do cidadão logado.
 */
const ITENS_MENU = PILARES.map((pilar) => ({ caminho: `/${pilar.id}`, rotulo: pilar.nome, cor: pilar.cor }));

function estaAtivo(caminho, rota) {
  return rota === caminho || rota.startsWith(`${caminho}/`);
}

// Páginas que ocupam a tela inteira abaixo do cabeçalho (sem rodapé e sem margens)
const TELA_CHEIA = new Set(["/mobilidade/mapa", "/mobilidade/rota"]);

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
          <a className="marca" href="#/" aria-label="Citizen Life, ir para o início">
            <Logo tamanho={38} />
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
                    style={{ "--cor-pilar": item.cor }}
                  >
                    <span className="menu__ponto" aria-hidden="true" />
                    {item.rotulo}
                  </a>
                </li>
              ))}
            </ul>

            <div className="sessao">
              {sessao ? (
                <>
                  <a
                    className="sessao__conta"
                    href="#/perfil"
                    aria-current={rota === "/perfil" || rota.startsWith("/perfil/") ? "page" : undefined}
                  >
                    <span className="sessao__avatar" aria-hidden="true">
                      {sessao.nome.charAt(0).toUpperCase()}
                    </span>
                    <span className="sessao__quem">
                      <span className="sessao__nome">{sessao.nome.split(" ")[0]}</span>
                      <span className="sessao__perfil">Meu perfil</span>
                    </span>
                  </a>
                  <button type="button" className="sessao__sair" onClick={sair}>
                    Sair
                  </button>
                </>
              ) : (
                <a
                  className="botao botao--entrar"
                  href="#/entrar"
                  aria-current={rota === "/entrar" ? "page" : undefined}
                >
                  <span className="botao--entrar__icone" aria-hidden="true" />
                  Entrar
                </a>
              )}
            </div>
          </nav>
        </div>
      </header>

      <main id="conteudo" className={`conteudo ${telaCheia ? "conteudo--tela-cheia" : ""}`} tabIndex={-1}>
        {children}
      </main>

      {!telaCheia && (
        <footer className="rodape">
          <div className="rodape__interno">
            <div className="rodape__marca">
              <Logo tamanho={34} />
              <p>O seu dia a dia na cidade, num lugar só.</p>
            </div>
            <nav className="rodape__pilares" aria-label="Pilares">
              <h2 className="rodape__titulo">Pilares</h2>
              <ul>
                {PILARES.map((pilar) => (
                  <li key={pilar.id}>
                    <a href={`#/${pilar.id}`}>{pilar.nome}</a>
                  </li>
                ))}
              </ul>
            </nav>
            <p className="rodape__credito">
              Global Solution 2026/2 · CitizenTech
              <br />
              Engenharia de Software FIAP
            </p>
          </div>
        </footer>
      )}
    </>
  );
}
