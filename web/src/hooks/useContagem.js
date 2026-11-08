import { useEffect, useRef, useState } from "react";
import { prefereMenosMovimento } from "../utils/movimento.js";

/**
 * Faz um número "contar" até o valor novo, com desaceleração no fim.
 * Usa requestAnimationFrame (nativo do navegador). Com movimento reduzido, troca direto.
 */
export function useContagem(alvo, duracao = 520) {
  const [valor, setValor] = useState(0);
  const atual = useRef(0);

  useEffect(() => {
    if (alvo === null || alvo === undefined) {
      return undefined;
    }
    if (prefereMenosMovimento()) {
      atual.current = alvo;
      setValor(alvo);
      return undefined;
    }

    const inicio = performance.now();
    const de = atual.current;
    let quadro;
    const passo = (agora) => {
      const progresso = Math.min(1, (agora - inicio) / duracao);
      const suave = 1 - (1 - progresso) ** 3;
      atual.current = de + (alvo - de) * suave;
      setValor(atual.current);
      if (progresso < 1) {
        quadro = requestAnimationFrame(passo);
      }
    };
    quadro = requestAnimationFrame(passo);
    return () => cancelAnimationFrame(quadro);
  }, [alvo, duracao]);

  return alvo === null || alvo === undefined ? null : valor;
}
