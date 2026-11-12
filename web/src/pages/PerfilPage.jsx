import { useEffect, useRef, useState } from "react";
import { Aviso, ExigirPerfil } from "../components/Estados.jsx";
import Icone from "../components/Icone.jsx";
import DadosConta from "../components/perfil/DadosConta.jsx";
import Enderecos from "../components/perfil/Enderecos.jsx";
import Locomocao from "../components/perfil/Locomocao.jsx";
import MinhasRotas from "../components/perfil/MinhasRotas.jsx";
import ResumoConta from "../components/perfil/ResumoConta.jsx";
import { useApp } from "../contexto.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { retirarAvisoPendente } from "../services/aviso.js";
import { PILARES, pilarPorId } from "../utils/pilares.js";
import NaoEncontradaPage from "./NaoEncontradaPage.jsx";

/** Seções da conta: #/perfil e #/perfil/dados. */
const CONTA = {
  "": { titulo: "Minha conta", Componente: ResumoConta },
  dados: { titulo: "Meus dados", Componente: DadosConta },
};

/** Abas de cada pilar que já funciona: #/perfil/<pilar>/<aba>. A primeira é a aberta por padrão. */
const ABAS = {
  mobilidade: [
    { id: "rotas", rotulo: "Minhas rotas", icone: "MAPA", Componente: MinhasRotas },
    { id: "locomocao", rotulo: "Como me locomovo", icone: "MOTO", Componente: Locomocao },
    { id: "enderecos", rotulo: "Meus endereços", icone: "PIN", Componente: Enderecos },
  ],
};

/**
 * Descobre o que mostrar a partir do caminho:
 * #/perfil · #/perfil/dados · #/perfil/<pilar> · #/perfil/<pilar>/<aba>
 */
function resolver({ pilar: idPilarComAba, secao }) {
  if (!idPilarComAba && (secao ?? "") in CONTA) {
    return { tipo: "conta", chave: secao ?? "", ...CONTA[secao ?? ""] };
  }
  const pilar = pilarPorId(idPilarComAba ?? secao);
  if (!pilar) return null;
  const abaPedida = idPilarComAba ? secao : null;
  if (!pilar.disponivel) {
    return abaPedida ? null : { tipo: "pilar", pilar, titulo: pilar.nome };
  }
  const abas = ABAS[pilar.id];
  const aba = abaPedida ? abas.find((item) => item.id === abaPedida) : abas[0];
  return aba ? { tipo: "pilar", pilar, abas, aba, titulo: `${aba.rotulo} · ${pilar.nome}` } : null;
}

export default function PerfilPage({ parametros }) {
  const alvo = resolver(parametros);
  useTitulo(alvo ? alvo.titulo : "Página não encontrada");

  if (!alvo) return <NaoEncontradaPage />;

  return (
    <ExigirPerfil motivo="Entre com a sua conta para ver o seu perfil, as rotas e os endereços salvos.">
      <Perfil alvo={alvo} />
    </ExigirPerfil>
  );
}

function Perfil({ alvo }) {
  const [aviso, setAviso] = useState(retirarAvisoPendente);

  return (
    <div className="perfil">
      <MenuPerfil alvo={alvo} />
      <div className="perfil__conteudo">
        <Aviso aviso={aviso} aoFechar={() => setAviso(null)} />
        {alvo.tipo === "conta" ? (
          <alvo.Componente setAviso={setAviso} />
        ) : (
          <SecaoPilar alvo={alvo} setAviso={setAviso} />
        )}
      </div>
    </div>
  );
}

/** Página de um pilar dentro do perfil: cabeçalho com a cor do pilar e as funções em abas. */
function SecaoPilar({ alvo, setAviso }) {
  const { pilar, abas, aba } = alvo;
  const refAbas = useRef(null);

  // No celular as abas rolam de lado: a aba aberta precisa aparecer, mesmo sendo a última
  useEffect(() => {
    const faixa = refAbas.current;
    const atual = faixa?.querySelector('[aria-current="page"]');
    if (faixa && atual) faixa.scrollLeft = atual.offsetLeft - 16;
  }, [aba?.id]);

  return (
    <section className="perfil-secao" aria-labelledby="perfil-pilar-titulo">
      <header className="perfil-pilar" style={{ "--cor-pilar": pilar.cor, "--cor-icone": pilar.corIcone }}>
        <span className="perfil-pilar__icone" aria-hidden="true">
          <Icone nome={pilar.icone} tamanho={24} />
        </span>
        <div>
          <h1 id="perfil-pilar-titulo" className="perfil-pilar__titulo">
            {pilar.nome}
            {!pilar.disponivel && <span className="perfil-menu__breve">Em breve</span>}
          </h1>
        </div>
      </header>

      {pilar.disponivel ? (
        <>
          <nav ref={refAbas} className="abas-perfil" aria-label={`Funções de ${pilar.nome}`}>
            {abas.map((item) => (
              <a
                key={item.id}
                className="abas-perfil__aba"
                href={`#/perfil/${pilar.id}/${item.id}`}
                aria-current={item.id === aba.id ? "page" : undefined}
              >
                <Icone nome={item.icone} tamanho={18} />
                {item.rotulo}
              </a>
            ))}
          </nav>
          <div className="abas-perfil__painel">
            <aba.Componente setAviso={setAviso} />
          </div>
        </>
      ) : (
        <>
          <div className="abas-perfil" aria-label={`Funções futuras de ${pilar.nome}`}>
            {pilar.perfil.map((rotulo) => (
              <span key={rotulo} className="abas-perfil__aba abas-perfil__aba--breve" aria-disabled="true">
                {rotulo}
              </span>
            ))}
          </div>
          <div className="abas-perfil__painel">
            <div className="perfil-breve" style={{ "--cor-pilar": pilar.cor }}>
              <span className="perfil-breve__icone" aria-hidden="true">
                <Icone nome={pilar.icone} tamanho={34} />
              </span>
              <p className="perfil-breve__titulo">Em construção</p>
              <a className="botao botao--secundario botao--pequeno" href={`#/${pilar.id}`}>
                Ver o pilar
              </a>
            </div>
          </div>
        </>
      )}
    </section>
  );
}

function MenuPerfil({ alvo }) {
  const { sessao, sair } = useApp();
  const pilarAtual = alvo.tipo === "pilar" ? alvo.pilar.id : null;

  return (
    <nav className="perfil-menu" aria-label="Seu perfil">
      <div className="perfil-menu__quem">
        <span className="perfil-menu__avatar" aria-hidden="true">
          {sessao.nome.charAt(0).toUpperCase()}
        </span>
        <span className="perfil-menu__nome">{sessao.nome}</span>
      </div>

      <div className="perfil-menu__grupo">
        <p className="perfil-menu__titulo">Minha conta</p>
        <ul>
          <ItemMenu caminho="/perfil" icone="RESUMO" rotulo="Resumo" ativo={alvo.chave === ""} />
          <ItemMenu caminho="/perfil/dados" icone="PESSOA" rotulo="Meus dados" ativo={alvo.chave === "dados"} />
        </ul>
      </div>

      <div className="perfil-menu__grupo">
        <p className="perfil-menu__titulo">Pilares</p>
        <ul>
          {PILARES.map((pilar) => (
            <ItemMenu
              key={pilar.id}
              caminho={`/perfil/${pilar.id}`}
              icone={pilar.icone}
              rotulo={pilar.nome}
              ativo={pilarAtual === pilar.id}
              cor={pilar.cor}
              breve={!pilar.disponivel}
            />
          ))}
        </ul>
      </div>

      <button type="button" className="perfil-menu__sair" onClick={sair}>
        Sair da conta
      </button>
    </nav>
  );
}

function ItemMenu({ caminho, icone, rotulo, ativo, cor, breve = false }) {
  return (
    <li>
      <a
        className={`perfil-menu__item ${breve ? "perfil-menu__item--breve" : ""}`}
        href={`#${caminho}`}
        aria-current={ativo ? "page" : undefined}
        style={cor ? { "--cor-pilar": cor } : undefined}
      >
        <Icone nome={icone} tamanho={18} />
        {rotulo}
        {breve && <span className="perfil-menu__breve">Em breve</span>}
      </a>
    </li>
  );
}
