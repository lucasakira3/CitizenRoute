package br.com.citizenroute.model.relatorio;

/** Linha do relatório "vias mais críticas". */
public record ViaCritica(long idVia, String via, String regiao, int totalOcorrencias,
                         int ocorrenciasAtivas, double gravidadeMedia, int gravidadeMaxima) {
}
