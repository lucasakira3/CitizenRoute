import Icone from "../components/Icone.jsx";
import { Carregando, ErroCarga, Vazio } from "../components/Estados.jsx";
import { useApp } from "../contexto.js";
import { useCarregar } from "../hooks/useCarregar.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { api } from "../services/api.js";

export default function AlimentacaoPage() {
  useTitulo("Alimentação");
  const { sessao } = useApp();
  const feiras = useCarregar(() => api.buscar("/feiras"), []);
  const feiraHoje = useCarregar(() => (sessao ? api.buscar(`/cidadaos/${sessao.id}/feira-hoje`) : Promise.resolve(null)), [sessao?.id]);

  return (
    <>
      <a className="voltar" href="#/">
        Todos os pilares
      </a>

      <section className="pilar-cabeca" aria-labelledby="alimentacao-titulo" style={{ "--cor-pilar": "var(--pilar-alimentacao)", "--cor-icone": "var(--asfalto)" }}>
        <span className="pilar-cabeca__icone">
          <Icone nome="ALIMENTACAO" tamanho={34} />
        </span>
        <div>
          <p className="sobretitulo">Pilar · Alimentação</p>
          <h1 id="alimentacao-titulo">Feiras livres</h1>
          <p className="lead">Descubra o que tem hoje perto de você, por zona e por dia da semana.</p>
        </div>
      </section>

      {sessao && (
        <section className="perfil-bloco" aria-labelledby="feira-hoje-titulo">
          <h2 id="feira-hoje-titulo" className="perfil-subtitulo">Feira hoje</h2>
          {feiraHoje.carregando && !feiraHoje.dados && <Carregando texto="Conferindo a sua feira de hoje…" />}
          {feiraHoje.erro && <ErroCarga erro={feiraHoje.erro} aoTentar={feiraHoje.recarregar} />}
          {feiraHoje.dados ? (
            <div className="cartao-perfil">
              <p className="rotas-salvas__trajeto">{feiraHoje.dados.rua}</p>
              <p className="rotas-salvas__meta">
                {feiraHoje.dados.nome} · {feiraHoje.dados.horario} · {feiraHoje.dados.zona}
              </p>
            </div>
          ) : (
            <Vazio titulo="Sem feira favorita hoje." />
          )}
        </section>
      )}

      <section className="perfil-bloco" aria-labelledby="feiras-titulo">
        <h2 id="feiras-titulo" className="perfil-subtitulo">Feiras disponíveis</h2>
        {feiras.carregando && !feiras.dados && <Carregando texto="Carregando feiras…" />}
        {feiras.erro && <ErroCarga erro={feiras.erro} aoTentar={feiras.recarregar} />}
        {feiras.dados && feiras.dados.length === 0 && <Vazio titulo="Nenhuma feira cadastrada no momento." />}

        <ul className="cartoes-perfil">
          {(feiras.dados ?? []).map((feira) => (
            <li key={feira.id} className="cartao-perfil">
              <div className="rotas-salvas__cabeca">
                <span className="rotas-salvas__nome">{feira.rua}</span>
                <span className="rotas-salvas__livre">{feira.dia}</span>
              </div>
              <p className="rotas-salvas__trajeto">{feira.nome}</p>
              <p className="rotas-salvas__meta">{feira.horario} · {feira.zona}</p>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
