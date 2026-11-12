package br.com.citizenlife.service;

import br.com.citizenlife.dao.FiltroOcorrencia;
import br.com.citizenlife.dao.OcorrenciaDao;
import br.com.citizenlife.exception.RecursoNaoEncontradoException;
import br.com.citizenlife.model.Ocorrencia;

import java.util.List;

/**
 * Consulta das ocorrências que aparecem no mapa de riscos.
 * As ocorrências vêm da carga do script.sql (relatos e registros dos órgãos da cidade).
 */
public class OcorrenciaService {

    private final OcorrenciaDao ocorrenciaDao;

    public OcorrenciaService(OcorrenciaDao ocorrenciaDao) {
        this.ocorrenciaDao = ocorrenciaDao;
    }

    /** Todas as ocorrências ou só as ativas (as que ainda afetam as vias). */
    public List<Ocorrencia> listar(boolean apenasAtivas) {
        return ocorrenciaDao.listar(apenasAtivas ? FiltroOcorrencia.ativas() : FiltroOcorrencia.todas());
    }

    public Ocorrencia buscar(long id) {
        return ocorrenciaDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ocorrência " + id + " não encontrada."));
    }
}
