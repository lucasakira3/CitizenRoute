package br.com.citizenroute.model.relatorio;

import br.com.citizenroute.model.Modal;

/**
 * Linha do relatório "modais mais expostos".
 * As contagens vêm do SQL (cidadãos do modal, rotas salvas e ocorrências ativas nas vias dessas rotas). O risco médio é calculado na RelatorioService com a matriz de risco.
 */
public record ExposicaoModal(Modal modal, int cidadaos, int rotas,
                             int ocorrenciasAtivasNasVias, double riscoMedioAtual) {

    public ExposicaoModal comRiscoMedio(double risco) {
        return new ExposicaoModal(modal, cidadaos, rotas, ocorrenciasAtivasNasVias, risco);
    }
}
