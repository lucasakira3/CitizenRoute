package br.com.citizenroute.model.relatorio;

import br.com.citizenroute.model.Modal;

/**
 * Linha do relatório "modais mais expostos".
 * As contagens vêm do SQL. O risco médio é calculado na RelatorioService com a matriz de risco.
 */
public record ExposicaoModal(Modal modal, int cidadaos, int viasMonitoradas,
                             int ocorrenciasAtivasNasVias, double riscoMedioAtual) {

    public ExposicaoModal comRiscoMedio(double risco) {
        return new ExposicaoModal(modal, cidadaos, viasMonitoradas, ocorrenciasAtivasNasVias, risco);
    }
}
