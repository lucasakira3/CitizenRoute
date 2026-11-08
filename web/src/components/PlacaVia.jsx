import { useApp } from "../contexto.js";
import { abreviarVia } from "../utils/formato.js";

/**
 * Mostra a via como uma placa de rua de São Paulo (azul, letras brancas).
 * Opcionalmente indica o nível de impacto da ocorrência naquela via.
 */
export default function PlacaVia({ via, impacto, apelido, semRegiao = false }) {
  const { rotulo } = useApp();
  return (
    <span className="placa-grupo">
      <span className="placa" title={via.nome}>
        <span className="placa__nome">{abreviarVia(via.nome)}</span>
        {!semRegiao && via.regiao && <span className="placa__regiao">{via.regiao.nome}</span>}
      </span>
      {apelido && <span className="placa-grupo__apelido">{apelido}</span>}
      {impacto && <span className={`impacto impacto--${impacto}`}>{rotulo("niveisImpacto", impacto)}</span>}
    </span>
  );
}
