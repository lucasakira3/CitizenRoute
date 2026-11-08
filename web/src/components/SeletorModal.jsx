import { useId } from "react";
import { useApp } from "../contexto.js";
import { MODAL_CURTO } from "../utils/rotulos.js";
import Icone from "./Icone.jsx";

/** Grupo de botões de rádio para escolher o modal (carro, moto, ônibus...). */
export default function SeletorModal({ valor, aoMudar, legenda = "Como você se move" }) {
  const { dominio } = useApp();
  const nome = useId();

  return (
    <fieldset className="segmentos">
      <legend className="segmentos__legenda">{legenda}</legend>
      <div className="segmentos__opcoes">
        {dominio.modais.map((modal) => (
          <label key={modal.codigo} className="segmentos__opcao" title={modal.rotulo}>
            <input
              type="radio"
              name={nome}
              value={modal.codigo}
              checked={valor === modal.codigo}
              onChange={() => aoMudar(modal.codigo)}
            />
            <span>
              <Icone nome={modal.codigo} tamanho={18} />
              {MODAL_CURTO[modal.codigo] ?? modal.rotulo}
            </span>
          </label>
        ))}
      </div>
    </fieldset>
  );
}
