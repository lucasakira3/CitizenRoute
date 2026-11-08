import { createContext, useContext } from "react";

/**
 * Dados que todas as páginas usam:
 * - sessao: usuário logado (ou null), entrar() e sair()
 * - dominio: opções vindas de GET /api/dominio (modais, tipos, status...)
 * - rotulo(grupo, codigo): texto amigável de um código, ex.: rotulo("modais", "ONIBUS")
 */
export const AppContext = createContext(null);

export function useApp() {
  return useContext(AppContext);
}
