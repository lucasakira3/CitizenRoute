package br.com.citizenroute.service;

import br.com.citizenroute.dao.ViaDao;
import br.com.citizenroute.exception.RecursoNaoEncontradoException;
import br.com.citizenroute.exception.ValidacaoException;
import br.com.citizenroute.model.Regiao;
import br.com.citizenroute.model.Via;

import java.util.List;
import java.util.Map;

/** Regras do cadastro de vias. Só gestores alteram. */
public class ViaService {

    private final ViaDao viaDao;
    private final RegiaoService regiaoService;
    private final AutenticacaoService autenticacao;

    public ViaService(ViaDao viaDao, RegiaoService regiaoService, AutenticacaoService autenticacao) {
        this.viaDao = viaDao;
        this.regiaoService = regiaoService;
        this.autenticacao = autenticacao;
    }

    public List<Via> listar(Long idRegiao, String busca) {
        return viaDao.listar(idRegiao, busca);
    }

    public Via buscar(long id) {
        return viaDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Via " + id + " não encontrada."));
    }

    public Via cadastrar(String nome, Long idRegiao, Long idAtor) {
        autenticacao.exigirGestor(idAtor, "cadastrar vias");
        Via via = validar(null, nome, idRegiao);
        return viaDao.inserir(via);
    }

    public Via atualizar(long id, String nome, Long idRegiao, Long idAtor) {
        autenticacao.exigirGestor(idAtor, "editar vias");
        Via via = validar(id, nome, idRegiao);
        if (!viaDao.atualizar(via)) {
            throw new RecursoNaoEncontradoException("Via " + id + " não encontrada.");
        }
        return via;
    }

    public void excluir(long id, Long idAtor) {
        autenticacao.exigirGestor(idAtor, "excluir vias");
        if (!viaDao.excluir(id)) {
            throw new RecursoNaoEncontradoException("Via " + id + " não encontrada.");
        }
    }

    private Via validar(Long id, String nome, Long idRegiao) {
        Validador validador = new Validador();
        String nomeLimpo = validador.texto("nome", nome, "Nome da via", 3, 120);
        validador.obrigatorio("idRegiao", idRegiao, "Escolha a região da via.");
        validador.validar();

        try {
            Regiao regiao = regiaoService.buscar(idRegiao);
            return new Via(id, nomeLimpo, regiao);
        } catch (RecursoNaoEncontradoException e) {
            throw new ValidacaoException("A região escolhida não existe mais.",
                    Map.of("idRegiao", "A região escolhida não existe mais."));
        }
    }
}
