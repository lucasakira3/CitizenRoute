package br.com.citizenroute.dao;

import br.com.citizenroute.model.StatusOcorrencia;
import br.com.citizenroute.model.TipoOcorrencia;

import java.util.Set;

/**
 * Filtros opcionais da listagem de ocorrências. Campo nulo significa "não filtrar".
 *
 * @param status             um status específico
 * @param tipos              um ou mais tipos (a fila do gestor usa os tipos do órgão dele)
 * @param idRegiao           ocorrências que atingem alguma via da região
 * @param idVia              ocorrências que atingem a via
 * @param idAutor            ocorrências reportadas por este cidadão
 * @param idCidadaoComRota   ocorrências que atingem alguma via das rotas salvas por este cidadão
 * @param apenasAtivas       somente ABERTA, CONFIRMADA ou EM_ATENDIMENTO
 */
public record FiltroOcorrencia(StatusOcorrencia status, Set<TipoOcorrencia> tipos, Long idRegiao, Long idVia,
                               Long idAutor, Long idCidadaoComRota, boolean apenasAtivas) {

    public static FiltroOcorrencia todas() {
        return new FiltroOcorrencia(null, null, null, null, null, null, false);
    }

    public static FiltroOcorrencia ativas() {
        return new FiltroOcorrencia(null, null, null, null, null, null, true);
    }

    public static FiltroOcorrencia ativasNasRotasDo(long idCidadao) {
        return new FiltroOcorrencia(null, null, null, null, null, idCidadao, true);
    }
}
