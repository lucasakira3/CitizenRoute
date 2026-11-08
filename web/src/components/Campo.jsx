import { cloneElement } from "react";

/**
 * Rótulo + controle + dica ou erro.
 * Liga o rótulo ao controle (htmlFor/id) e anuncia o erro para leitores de tela (aria-describedby).
 */
export default function Campo({ id, rotulo, erro, dica, opcional = false, children, className = "" }) {
  const idDescricao = erro ? `${id}-erro` : dica ? `${id}-dica` : undefined;
  const controle = cloneElement(children, {
    id,
    "aria-invalid": erro ? true : undefined,
    "aria-describedby": idDescricao,
  });

  return (
    <div className={`campo ${erro ? "campo--erro" : ""} ${className}`}>
      <label className="campo__rotulo" htmlFor={id}>
        {rotulo}
        {opcional && <span className="campo__opcional"> (opcional)</span>}
      </label>
      {controle}
      {erro ? (
        <p className="campo__erro" id={`${id}-erro`}>
          {erro}
        </p>
      ) : (
        dica && (
          <p className="campo__dica" id={`${id}-dica`}>
            {dica}
          </p>
        )
      )}
    </div>
  );
}
