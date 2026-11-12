import { useEffect, useState } from "react";
import { flushSync } from "react-dom";
import { prefereMenosMovimento } from "../utils/movimento.js";

// Navegação por hash (#/mobilidade): funciona sem configurar o servidor e sem biblioteca extra.

function lerRota() {
  const hash = window.location.hash.replace(/^#/, "");
  const caminho = hash.split("?")[0];
  return caminho || "/";
}

export function useRota() {
  const [rota, setRota] = useState(lerRota);

  useEffect(() => {
    const aoMudar = () => {
      const nova = lerRota();
      // View Transitions API (nativa no Chrome e no Edge): a página antiga some e a nova entra suavemente.
      // Em navegadores sem suporte, ou com movimento reduzido, a troca é imediata.
      if (document.startViewTransition && !prefereMenosMovimento()) {
        document.startViewTransition(() => flushSync(() => setRota(nova)));
      } else {
        setRota(nova);
      }
    };
    window.addEventListener("hashchange", aoMudar);
    return () => window.removeEventListener("hashchange", aoMudar);
  }, []);

  return rota;
}

export function navegar(caminho) {
  window.location.hash = caminho;
}

/**
 * Compara a rota atual com um padrão como "/:pilar".
 * Devolve os parâmetros ({ id: "12" }) ou null se não combinar.
 */
export function combinar(padrao, rota) {
  const partesPadrao = padrao.split("/").filter(Boolean);
  const partesRota = rota.split("/").filter(Boolean);
  if (partesPadrao.length !== partesRota.length) {
    return null;
  }
  const parametros = {};
  for (let i = 0; i < partesPadrao.length; i += 1) {
    if (partesPadrao[i].startsWith(":")) {
      parametros[partesPadrao[i].slice(1)] = decodeURIComponent(partesRota[i]);
    } else if (partesPadrao[i] !== partesRota[i]) {
      return null;
    }
  }
  return parametros;
}
