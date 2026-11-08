// Textos de interface que dependem de um código do domínio.

/** Rótulo curto de cada modal, para botões e seletores. */
export const MODAL_CURTO = {
  CARRO: "Carro",
  MOTO: "Moto",
  ONIBUS: "Ônibus",
  APP: "App",
  PEDESTRE: "A pé",
  BIKE: "Bike",
};

/** Frase usada em "risco para quem ___". */
export const MODAL_FRASE = {
  CARRO: "anda de carro",
  MOTO: "anda de moto",
  ONIBUS: "anda de ônibus",
  APP: "dirige por aplicativo",
  PEDESTRE: "anda a pé",
  BIKE: "anda de bicicleta",
};

/** O que cada mudança de status significa para o gestor. */
export const ACAO_STATUS = {
  CONFIRMADA: {
    botao: "Confirmar",
    feito: "Ocorrência confirmada.",
    pergunta: "O que foi verificado?",
    exemplo: "Ex.: Confirmado pelas câmeras da CET.",
  },
  EM_ATENDIMENTO: {
    botao: "Iniciar atendimento",
    feito: "Atendimento iniciado.",
    pergunta: "Quem está atendendo e como?",
    exemplo: "Ex.: Equipe de poda a caminho do local.",
  },
  RESOLVIDA: {
    botao: "Marcar como resolvida",
    feito: "Ocorrência marcada como resolvida.",
    pergunta: "O que foi feito para resolver?",
    exemplo: "Ex.: Pista limpa e faixas liberadas.",
  },
  DESCARTADA: {
    botao: "Descartar",
    feito: "Ocorrência descartada.",
    pergunta: "Por que o relato não procede?",
    exemplo: "Ex.: Relato duplicado de outra ocorrência.",
  },
};

export const GRAVIDADE = {
  1: "Leve",
  2: "Moderada",
  3: "Séria",
  4: "Grave",
  5: "Muito grave",
};
