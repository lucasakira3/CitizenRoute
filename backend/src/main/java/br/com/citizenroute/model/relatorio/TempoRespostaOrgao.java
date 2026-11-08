package br.com.citizenroute.model.relatorio;

import br.com.citizenroute.model.Orgao;

/** Linha do relatório "tempo médio de resposta por órgão". */
public record TempoRespostaOrgao(Orgao orgao, int ocorrenciasAtendidas, double minutosMedios, int resolvidas) {
}
