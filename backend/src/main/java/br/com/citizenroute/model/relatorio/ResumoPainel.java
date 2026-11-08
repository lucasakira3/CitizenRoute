package br.com.citizenroute.model.relatorio;

/** Números do painel inicial. O tempo médio fica nulo enquanto nenhuma ocorrência tiver ação. */
public record ResumoPainel(int ocorrenciasAtivas, int aguardandoConfirmacao, int emAtendimento,
                           int gravesAtivas, int resolvidasUltimos30Dias, Double tempoMedioRespostaMinutos,
                           int cidadaos, int viasMonitoradas) {
}
