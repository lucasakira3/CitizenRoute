import { useApp } from "../contexto.js";
import { formatarNumero } from "../utils/formato.js";

/**
 * O risco aparece como uma placa de advertência (losango), na cor da faixa:
 * baixo (verde), moderado (amarelo), alto (laranja) e crítico (vermelho).
 */
export default function SinalRisco({ risco, tamanho = "medio" }) {
  const { rotulo } = useApp();
  if (!risco) {
    return null;
  }
  const valor = formatarNumero(risco.valor, 1);
  const nivel = rotulo("niveisRisco", risco.nivel);
  return (
    <span
      className={`sinal sinal--${risco.nivel} sinal--${tamanho}`}
      role="img"
      aria-label={`Risco ${nivel.toLowerCase()}, ${valor} de 18`}
      title={`Risco ${nivel.toLowerCase()}`}
    >
      <span className="sinal__valor" aria-hidden="true">
        {valor}
      </span>
    </span>
  );
}
