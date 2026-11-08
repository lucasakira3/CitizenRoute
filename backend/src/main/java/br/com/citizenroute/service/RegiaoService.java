package br.com.citizenroute.service;

import br.com.citizenroute.dao.RegiaoDao;
import br.com.citizenroute.exception.RecursoNaoEncontradoException;
import br.com.citizenroute.model.Regiao;

import java.util.List;

/** Regras do cadastro de regiões. Só gestores alteram. */
public class RegiaoService {

    private final RegiaoDao regiaoDao;
    private final AutenticacaoService autenticacao;

    public RegiaoService(RegiaoDao regiaoDao, AutenticacaoService autenticacao) {
        this.regiaoDao = regiaoDao;
        this.autenticacao = autenticacao;
    }

    public List<Regiao> listar() {
        return regiaoDao.listar();
    }

    public Regiao buscar(long id) {
        return regiaoDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Região " + id + " não encontrada."));
    }

    public Regiao cadastrar(String nome, Long idAtor) {
        autenticacao.exigirGestor(idAtor, "cadastrar regiões");
        return regiaoDao.inserir(new Regiao(null, validarNome(nome)));
    }

    public Regiao atualizar(long id, String nome, Long idAtor) {
        autenticacao.exigirGestor(idAtor, "editar regiões");
        Regiao regiao = new Regiao(id, validarNome(nome));
        if (!regiaoDao.atualizar(regiao)) {
            throw new RecursoNaoEncontradoException("Região " + id + " não encontrada.");
        }
        return regiao;
    }

    public void excluir(long id, Long idAtor) {
        autenticacao.exigirGestor(idAtor, "excluir regiões");
        if (!regiaoDao.excluir(id)) {
            throw new RecursoNaoEncontradoException("Região " + id + " não encontrada.");
        }
    }

    private String validarNome(String nome) {
        Validador validador = new Validador();
        String limpo = validador.texto("nome", nome, "Nome da região", 2, 60);
        validador.validar();
        return limpo;
    }
}
