package br.com.citizenlife.dao;

/**
 * Filtros opcionais da listagem de ocorrências.
 *
 * @param apenasAtivas     somente ABERTA, CONFIRMADA ou EM_ATENDIMENTO
 * @param idCidadaoComRota ocorrências que atingem alguma via das rotas salvas por este cidadão (nulo: todas)
 */
public record FiltroOcorrencia(boolean apenasAtivas, Long idCidadaoComRota) {

    public static FiltroOcorrencia todas() {
        return new FiltroOcorrencia(false, null);
    }

    public static FiltroOcorrencia ativas() {
        return new FiltroOcorrencia(true, null);
    }

    public static FiltroOcorrencia ativasNasRotasDo(long idCidadao) {
        return new FiltroOcorrencia(true, idCidadao);
    }
}
