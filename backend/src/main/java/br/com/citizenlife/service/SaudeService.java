package br.com.citizenlife.service;

import br.com.citizenlife.dao.SaudeDao;
import br.com.citizenlife.exception.PersistenciaException;

import java.util.Optional;

/** Diagnóstico usado pelo endpoint /api/health e pela faixa de status do front-end. */
public class SaudeService {

    private final SaudeDao saudeDao;

    public SaudeService(SaudeDao saudeDao) {
        this.saudeDao = saudeDao;
    }

    /** Vazio quando o banco está pronto; caso contrário, a explicação do problema. */
    public Optional<String> problemaNoBanco() {
        try {
            saudeDao.verificar();
            return Optional.empty();
        } catch (PersistenciaException e) {
            return Optional.of(e.getMessage());
        }
    }

    public String origemConfiguracao() {
        return saudeDao.origemConfiguracao();
    }
}
