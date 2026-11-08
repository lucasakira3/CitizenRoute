import { useLayoutEffect, useRef } from "react";
import { prefereMenosMovimento } from "../utils/movimento.js";

/**
 * Anima a troca de posição dos itens de uma lista (técnica FLIP), com a Web Animations API do navegador.
 *
 * 1. Depois de cada renderização, guarda onde cada item ficou (data-chave → posição).
 * 2. Na renderização seguinte, se um item mudou de lugar, ele começa na posição antiga
 *    e desliza até a nova.
 *
 * O contêiner precisa de position: relative, porque a posição é medida com offsetTop.
 */
export function useReordenacao(referencia, dependencia) {
  const posicoes = useRef(new Map());

  useLayoutEffect(() => {
    const lista = referencia.current;
    if (!lista) {
      return;
    }
    const itens = [...lista.querySelectorAll(":scope > [data-chave]")];
    const animar = !prefereMenosMovimento() && typeof Element.prototype.animate === "function";

    itens.forEach((item) => {
      const anterior = posicoes.current.get(item.dataset.chave);
      const deslocamento = anterior === undefined ? 0 : anterior - item.offsetTop;
      if (animar && Math.abs(deslocamento) > 1) {
        item.animate([{ transform: `translateY(${deslocamento}px)` }, { transform: "translateY(0)" }], {
          duration: 460,
          easing: "cubic-bezier(0.2, 0.8, 0.2, 1)",
        });
      }
    });

    posicoes.current = new Map(itens.map((item) => [item.dataset.chave, item.offsetTop]));
  }, [referencia, dependencia]);
}
