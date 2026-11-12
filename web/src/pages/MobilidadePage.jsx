import Icone from "../components/Icone.jsx";
import { useApp } from "../contexto.js";
import { useTitulo } from "../hooks/useTitulo.js";

const OPCOES = [
  {
    caminho: "/mobilidade/rota",
    icone: "MAPA",
    titulo: "Planejar rota",
    texto: "A rota mais rápida e a mais segura, com o risco do caminho.",
    acao: "Planejar",
  },
  {
    caminho: "/mobilidade/mapa",
    icone: "ALVO",
    titulo: "Mapa de riscos",
    texto: "As ocorrências da cidade em 3D, na cor do risco.",
    acao: "Abrir",
  },
];

export default function MobilidadePage() {
  useTitulo("Mobilidade");
  const { dominio } = useApp();
  const limites = dominio.limitesRisco;

  return (
    <>
      <a className="voltar" href="#/">
        Todos os pilares
      </a>

      <section className="pilar-cabeca" aria-labelledby="mobilidade-titulo">
        <span className="pilar-cabeca__icone">
          <Icone nome="MOBILIDADE" tamanho={34} />
        </span>
        <div>
          <h1 id="mobilidade-titulo">Mobilidade</h1>
          <p className="lead">O risco do caminho, calculado para o seu meio de transporte.</p>
        </div>
      </section>

      <ul className="opcoes-pilar">
        {OPCOES.map((opcao) => (
          <li key={opcao.caminho}>
            <a className="opcao-pilar" href={`#${opcao.caminho}`}>
              <span className="opcao-pilar__icone">
                <Icone nome={opcao.icone} tamanho={28} />
              </span>
              <span className="opcao-pilar__titulo">{opcao.titulo}</span>
              <span className="opcao-pilar__texto">{opcao.texto}</span>
              <span className="opcao-pilar__acao">
                {opcao.acao}
                <Icone nome="SETA" tamanho={18} />
              </span>
            </a>
          </li>
        ))}
      </ul>

      <section className="regra-risco" aria-labelledby="regra-titulo">
        <h2 id="regra-titulo" className="visualmente-oculto">
          Níveis de risco
        </h2>
        <span className="regra-risco__rotulo" aria-hidden="true">
          Risco
        </span>
        <ul className="regra-risco__faixas">
          <li className="regra-risco__faixa regra-risco__faixa--BAIXO">
            <strong>Baixo</strong> &lt; {limites.moderado}
          </li>
          <li className="regra-risco__faixa regra-risco__faixa--MODERADO">
            <strong>Moderado</strong> {limites.moderado}+
          </li>
          <li className="regra-risco__faixa regra-risco__faixa--ALTO">
            <strong>Alto</strong> {limites.alto}+
          </li>
          <li className="regra-risco__faixa regra-risco__faixa--CRITICO">
            <strong>Crítico</strong> {limites.critico}+
          </li>
        </ul>
      </section>
    </>
  );
}
