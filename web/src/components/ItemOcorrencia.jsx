import { useApp } from "../contexto.js";
import { tempoRelativo } from "../utils/formato.js";
import { EtiquetaStatus, Gravidade } from "./Etiquetas.jsx";
import Icone from "./Icone.jsx";
import PlacaVia from "./PlacaVia.jsx";
import SinalRisco from "./SinalRisco.jsx";

/**
 * Uma ocorrência em lista: risco (se houver), título, tipo, status, vias e quando foi registrada.
 * "indice" escalona a animação de entrada; "data-chave" permite animar a reordenação.
 */
export default function ItemOcorrencia({ ocorrencia, risco, indice = 0, children }) {
  const { rotulo } = useApp();
  return (
    <article
      className={`item-ocorrencia ${risco ? "item-ocorrencia--com-risco" : ""}`}
      data-chave={ocorrencia.id}
      style={{ "--i": Math.min(indice, 10) }}
    >
      {risco && (
        <div className="item-ocorrencia__risco">
          {/* A key muda com o valor: o losango "vira" quando o risco é recalculado */}
          <SinalRisco key={`${risco.nivel}-${risco.valor}`} risco={risco} />
        </div>
      )}
      <div className="item-ocorrencia__corpo">
        <p className="item-ocorrencia__tipo">
          <Icone nome={ocorrencia.tipo} tamanho={16} className="item-ocorrencia__icone" />
          {rotulo("tiposOcorrencia", ocorrencia.tipo)}
          <span aria-hidden="true"> · </span>
          <span className="item-ocorrencia__quando">{tempoRelativo(ocorrencia.dataRegistro)}</span>
        </p>
        <h3 className="item-ocorrencia__titulo">
          <a href={`#/ocorrencias/${ocorrencia.id}`}>{ocorrencia.titulo}</a>
        </h3>
        <div className="item-ocorrencia__vias">
          {ocorrencia.vias.map((item) => (
            <PlacaVia key={item.via.id} via={item.via} impacto={item.nivelImpacto} />
          ))}
        </div>
        <div className="item-ocorrencia__rodape">
          <EtiquetaStatus status={ocorrencia.status} />
          <Gravidade valor={ocorrencia.gravidade} />
        </div>
        {children}
      </div>
    </article>
  );
}
