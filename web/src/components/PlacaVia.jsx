import { abreviarVia } from "../utils/formato.js";

/** Mostra a via como uma placa de rua de São Paulo (azul, letras brancas), com a zona embaixo. */
export default function PlacaVia({ via, apelido, semRegiao = false }) {
  return (
    <span className="placa-grupo">
      <span className="placa" title={via.nome}>
        <span className="placa__nome">{abreviarVia(via.nome)}</span>
        {!semRegiao && via.zona && <span className="placa__regiao">{via.zona}</span>}
      </span>
      {apelido && <span className="placa-grupo__apelido">{apelido}</span>}
    </span>
  );
}
