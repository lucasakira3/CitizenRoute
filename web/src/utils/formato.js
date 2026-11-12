// Formatação de datas e números no padrão brasileiro.

/** "há 40 min", "há 3 h", "há 5 dias". */
export function tempoRelativo(iso) {
  if (!iso) {
    return "";
  }
  const minutos = Math.round((Date.now() - new Date(iso).getTime()) / 60000);
  if (minutos < 1) {
    return "agora";
  }
  if (minutos < 60) {
    return `há ${minutos} min`;
  }
  const horas = Math.round(minutos / 60);
  if (horas < 24) {
    return `há ${horas} h`;
  }
  const dias = Math.round(horas / 24);
  return dias === 1 ? "há 1 dia" : `há ${dias} dias`;
}

export function formatarNumero(valor, casas = 0) {
  if (valor === null || valor === undefined || Number.isNaN(valor)) {
    return "—";
  }
  return Number(valor).toLocaleString("pt-BR", { minimumFractionDigits: casas, maximumFractionDigits: casas });
}

/** 18 → "18 min"; 125 → "2 h 05 min". */
export function formatarMinutos(minutos) {
  if (minutos === null || minutos === undefined) {
    return "—";
  }
  const total = Math.round(minutos);
  if (total < 60) {
    return `${total} min`;
  }
  const horas = Math.floor(total / 60);
  const resto = String(total % 60).padStart(2, "0");
  return `${horas} h ${resto} min`;
}

/** Abrevia como nas placas de rua de São Paulo: "Avenida Paulista" → "Av. Paulista". */
export function abreviarVia(nome) {
  return nome
    .replace(/^Avenida\s/i, "Av. ")
    .replace(/^Rua\s/i, "R. ")
    .replace(/\bBrigadeiro\s/i, "Brig. ")
    .replace(/\bEngenheiro\s/i, "Eng. ");
}
