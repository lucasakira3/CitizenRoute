import Icone from "../components/Icone.jsx";
import { useTitulo } from "../hooks/useTitulo.js";
import { pilarPorId } from "../utils/pilares.js";
import NaoEncontradaPage from "./NaoEncontradaPage.jsx";

/** Pilares que ainda não têm consulta: mostra o que a primeira consulta vai trazer. */
export default function EmBrevePage({ parametros }) {
  const pilar = pilarPorId(parametros.pilar);
  useTitulo(pilar && !pilar.disponivel ? pilar.nome : "Página não encontrada");

  if (!pilar || pilar.disponivel) {
    return <NaoEncontradaPage />;
  }

  return (
    <>
      <a className="voltar" href="#/">
        Todos os pilares
      </a>

      <section
        className="pilar-cabeca"
        aria-labelledby="pilar-titulo"
        style={{ "--cor-pilar": pilar.cor, "--cor-icone": pilar.corIcone }}
      >
        <span className="pilar-cabeca__icone">
          <Icone nome={pilar.icone} tamanho={34} />
        </span>
        <div>
          <p className="sobretitulo">
            Pilar · {pilar.nome}
            <span className="pilar-cabeca__selo">Em breve</span>
          </p>
          <h1 id="pilar-titulo">{pilar.consulta}</h1>
          <p className="lead">{pilar.resumo}</p>
        </div>
      </section>

      <section className="em-breve" aria-labelledby="em-breve-titulo">
        <h2 id="em-breve-titulo">O que vem</h2>
        <ul className="em-breve__lista">
          {pilar.planos.map((plano) => (
            <li key={plano}>{plano}</li>
          ))}
        </ul>
        <div className="acoes">
          <a className="botao botao--primario" href="#/mobilidade">
            Ir para Mobilidade
          </a>
          <a className="botao botao--secundario" href="#/">
            Voltar
          </a>
        </div>
      </section>
    </>
  );
}
