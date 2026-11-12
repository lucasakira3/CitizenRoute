package br.com.citizenlife.service;

import br.com.citizenlife.dao.CidadaoDao;
import br.com.citizenlife.exception.RecursoNaoEncontradoException;
import br.com.citizenlife.model.Cidadao;
import br.com.citizenlife.model.Modal;

import java.time.LocalDateTime;

/**
 * Regras do cadastro de cidadãos.
 * Qualquer pessoa pode se cadastrar; ver, editar ou excluir a conta é permitido só ao próprio cidadão.
 */
public class CidadaoService {

    private final CidadaoDao cidadaoDao;
    private final SenhaService senhaService;
    private final AutenticacaoService autenticacao;

    public CidadaoService(CidadaoDao cidadaoDao, SenhaService senhaService, AutenticacaoService autenticacao) {
        this.cidadaoDao = cidadaoDao;
        this.senhaService = senhaService;
        this.autenticacao = autenticacao;
    }

    /** Busca sem conferir permissão: uso interno das outras services. */
    public Cidadao buscar(long id) {
        return cidadaoDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cidadão " + id + " não encontrado."));
    }

    /** Dados da própria conta. */
    public Cidadao buscar(long id, Long idAtor) {
        autenticacao.exigirProprio(idAtor, id, "ver os dados");
        return buscar(id);
    }

    /**
     * Cadastro público e simples: nome, e-mail e senha. O modal é opcional aqui
     * e é configurado depois, no perfil.
     */
    public Cidadao cadastrar(String nome, String email, Modal modal, String senha) {
        Validador validador = new Validador();
        Cidadao cidadao = new Cidadao();
        cidadao.setNome(validador.texto("nome", nome, "Nome", 3, 100));
        cidadao.setEmail(validador.email("email", email));
        validador.senha("senha", senha);
        if (!validador.temErro("email") && cidadaoDao.emailEmUso(cidadao.getEmail(), null)) {
            validador.erro("email", "Este e-mail já está cadastrado.");
        }
        validador.validar();

        cidadao.setModal(modal);
        cidadao.setSenhaHash(senhaService.gerarHash(senha));
        cidadao.setDataCadastro(LocalDateTime.now());
        return cidadaoDao.inserir(cidadao);
    }

    /** Atualiza nome, e-mail e modal (opcional). A senha só muda quando uma nova é informada. */
    public Cidadao atualizar(long id, String nome, String email, Modal modal, String novaSenha, Long idAtor) {
        autenticacao.exigirProprio(idAtor, id, "editar os dados");
        Cidadao atual = buscar(id);

        Validador validador = new Validador();
        atual.setNome(validador.texto("nome", nome, "Nome", 3, 100));
        atual.setEmail(validador.email("email", email));
        boolean trocarSenha = novaSenha != null && !novaSenha.isEmpty();
        if (trocarSenha) {
            validador.senha("senha", novaSenha);
        }
        if (!validador.temErro("email") && cidadaoDao.emailEmUso(atual.getEmail(), id)) {
            validador.erro("email", "Este e-mail já está cadastrado.");
        }
        validador.validar();

        atual.setModal(modal);
        if (trocarSenha) {
            atual.setSenhaHash(senhaService.gerarHash(novaSenha));
        }
        if (!cidadaoDao.atualizar(atual)) {
            throw new RecursoNaoEncontradoException("Cidadão " + id + " não encontrado.");
        }
        return atual;
    }

    public void excluir(long id, Long idAtor) {
        autenticacao.exigirProprio(idAtor, id, "excluir os dados");
        if (!cidadaoDao.excluir(id)) {
            throw new RecursoNaoEncontradoException("Cidadão " + id + " não encontrado.");
        }
    }
}
