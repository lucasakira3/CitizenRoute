import { useEffect } from "react";

export function useTitulo(titulo) {
  useEffect(() => {
    document.title = titulo ? `${titulo} · Citizen Life` : "Citizen Life · seu dia a dia na cidade";
  }, [titulo]);
}
