package br.com.citizenroute.service;

import br.com.citizenroute.dao.CidadaoDao;
import br.com.citizenroute.dao.UsuarioDao;
import br.com.citizenroute.exception.RecursoNaoEncontradoException;
import br.com.citizenroute.model.Cidadao;
import br.com.citizenroute.model.Modal;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras do cadastro de cidadãos.
 * Qualquer pessoa pode se cadastrar; editar ou excluir é permitido ao próprio cidadão ou a um gestor.
 */
public class CidadaoService {

    private final CidadaoDao cidadaoDao;
    private final UsuarioDao usuarioDao;
    private final SenhaService senhaService;
    private final AutenticacaoService autenticacao;

    public CidadaoService(CidadaoDao cidadaoDao, UsuarioDao usuarioDao, SenhaService senhaService,
                          AutenticacaoService autenticacao) {
        this.cidadaoDao = cidadaoDao;
        this.usuarioDao = usuarioDao;
        this.senhaService = senhaService;
        this.autenticacao = autenticacao;
    }

    public List<Cidadao> listar() {
        return cidadaoDao.listar();
    }

    public Cidadao buscar(long id) {
        return cidadaoDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cidadão " + id + " não encontrado."));
    }

    /** Cadastro público: é assim que um novo cidadão entra no sistema. */
    public Cidadao cadastrar(String nome, String email, Modal modal, String senha) {
        Validador validador = new Validador();
        Cidadao cidadao = new Cidadao();
        cidadao.setNome(validador.texto("nome", nome, "Nome", 3, 100));
        cidadao.setEmail(validador.email("email", email));
        validador.obrigatorio("modal", modal, "Escolha como você se locomove.");
        validador.senha("senha", senha);
        if (!validador.temErro("email") && usuarioDao.emailEmUso(cidadao.getEmail(), null)) {
            validador.erro("email", "Este e-mail já está cadastrado.");
        }
        validador.validar();

        cidadao.setModal(modal);
        cidadao.setSenhaHash(senhaService.gerarHash(senha));
        cidadao.setDataCadastro(LocalDateTime.now());
        return cidadaoDao.inserir(cidadao);
    }

    /** Atualiza nome, e-mail e modal. A senha só muda quando uma nova é informada. */
    public Cidadao atualizar(long id, String nome, String email, Modal modal, String novaSenha, Long idAtor) {
        autenticacao.exigirProprioOuGestor(idAtor, id, "editar os dados");
        Cidadao atual = buscar(id);

        Validador validador = new Validador();
        atual.setNome(validador.texto("nome", nome, "Nome", 3, 100));
        atual.setEmail(validador.email("email", email));
        validador.obrigatorio("modal", modal, "Escolha como o cidadão se locomove.");
        boolean trocarSenha = novaSenha != null && !novaSenha.isEmpty();
        if (trocarSenha) {
            validador.senha("senha", novaSenha);
        }
        if (!validador.temErro("email") && usuarioDao.emailEmUso(atual.getEmail(), id)) {
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
        autenticacao.exigirProprioOuGestor(idAtor, id, "excluir os dados");
        if (!cidadaoDao.excluir(id)) {
            throw new RecursoNaoEncontradoException("Cidadão " + id + " não encontrado.");
        }
    }
}
