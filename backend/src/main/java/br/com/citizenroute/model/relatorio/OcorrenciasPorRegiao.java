package br.com.citizenroute.model.relatorio;

import br.com.citizenroute.model.TipoOcorrencia;

/** Linha do relatório "ocorrências por região e tipo". */
public record OcorrenciasPorRegiao(String regiao, TipoOcorrencia tipo, int total, int ativas) {
}
