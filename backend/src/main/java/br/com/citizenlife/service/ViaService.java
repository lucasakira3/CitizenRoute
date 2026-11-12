package br.com.citizenlife.service;

import br.com.citizenlife.dao.ViaDao;
import br.com.citizenlife.exception.RecursoNaoEncontradoException;
import br.com.citizenlife.model.Via;

import java.util.List;

/** Consulta das vias cadastradas, que o mapa usa para ligar o caminho às ocorrências. */
public class ViaService {

    private final ViaDao viaDao;

    public ViaService(ViaDao viaDao) {
        this.viaDao = viaDao;
    }

    public List<Via> listar() {
        return viaDao.listar();
    }

    public Via buscar(long id) {
        return viaDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Via " + id + " não encontrada."));
    }
}
