import { GRAVIDADE } from "../utils/rotulos.js";

/** Cinco marcas, preenchidas até a gravidade informada. */
export function Gravidade({ valor }) {
  return (
    <span className="gravidade" role="img" aria-label={`Gravidade ${valor} de 5: ${GRAVIDADE[valor]}`}>
      {[1, 2, 3, 4, 5].map((nivel) => (
        <span key={nivel} className={nivel <= valor ? "gravidade__marca gravidade__marca--ativa" : "gravidade__marca"} />
      ))}
      <span className="gravidade__texto" aria-hidden="true">
        {GRAVIDADE[valor]}
      </span>
    </span>
  );
}
