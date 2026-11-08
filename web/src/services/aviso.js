// Guarda uma mensagem para ser exibida na próxima página.
// Exemplo: depois de salvar uma ocorrência, a página de detalhe mostra "Ocorrência registrada."

let pendente = null;

export function avisarNaProximaPagina(tipo, texto) {
  pendente = { tipo, texto };
}

export function retirarAvisoPendente() {
  const aviso = pendente;
  pendente = null;
  return aviso;
}
