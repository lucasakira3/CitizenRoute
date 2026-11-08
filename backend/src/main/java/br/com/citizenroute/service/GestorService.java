package br.com.citizenroute.service;

import br.com.citizenroute.dao.GestorDao;
import br.com.citizenroute.dao.UsuarioDao;
import br.com.citizenroute.exception.RecursoNaoEncontradoException;
import br.com.citizenroute.exception.RegraNegocioException;
import br.com.citizenroute.model.Gestor;
import br.com.citizenroute.model.Orgao;

import java.time.LocalDateTime;
import java.util.List;

/** Regras do cadastro de gestores. Só um gestor cadastra, edita ou exclui outro gestor. */
public class GestorService {

    private final GestorDao gestorDao;
    private final UsuarioDao usuarioDao;
    private final SenhaService senhaService;
    private final AutenticacaoService autenticacao;

    public GestorService(GestorDao gestorDao, UsuarioDao usuarioDao, SenhaService senhaService,
                         AutenticacaoService autenticacao) {
        this.gestorDao = gestorDao;
        this.usuarioDao = usuarioDao;
        this.senhaService = senhaService;
        this.autenticacao = autenticacao;
    }

    public List<Gestor> listar() {
        return gestorDao.listar();
    }

    public Gestor buscar(long id) {
        return gestorDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor " + id + " não encontrado."));
    }

    public Gestor cadastrar(String nome, String email, Orgao orgao, String matricula, String senha, Long idAtor) {
        autenticacao.exigirGestor(idAtor, "cadastrar gestores");
        Validador validador = new Validador();
        Gestor gestor = new Gestor();
        preencher(validador, gestor, null, nome, email, orgao, matricula);
        validador.senha("senha", senha);
        validador.validar();

        gestor.setSenhaHash(senhaService.gerarHash(senha));
        gestor.setDataCadastro(LocalDateTime.now());
        return gestorDao.inserir(gestor);
    }

    public Gestor atualizar(long id, String nome, String email, Orgao orgao, String matricula,
                            String novaSenha, Long idAtor) {
        autenticacao.exigirGestor(idAtor, "editar gestores");
        Gestor atual = buscar(id);

        Validador validador = new Validador();
        preencher(validador, atual, id, nome, email, orgao, matricula);
        boolean trocarSenha = novaSenha != null && !novaSenha.isEmpty();
        if (trocarSenha) {
            validador.senha("senha", novaSenha);
        }
        validador.validar();

        if (trocarSenha) {
            atual.setSenhaHash(senhaService.gerarHash(novaSenha));
        }
        if (!gestorDao.atualizar(atual)) {
            throw new RecursoNaoEncontradoException("Gestor " + id + " não encontrado.");
        }
        return atual;
    }

    public void excluir(long id, Long idAtor) {
        Gestor ator = autenticacao.exigirGestor(idAtor, "excluir gestores");
        if (ator.getId() == id) {
            throw new RegraNegocioException("Você não pode excluir a própria conta de gestor.");
        }
        if (!gestorDao.excluir(id)) {
            throw new RecursoNaoEncontradoException("Gestor " + id + " não encontrado.");
        }
    }

    private void preencher(Validador validador, Gestor gestor, Long id, String nome, String email,
                           Orgao orgao, String matricula) {
        gestor.setNome(validador.texto("nome", nome, "Nome", 3, 100));
        gestor.setEmail(validador.email("email", email));
        gestor.setMatricula(validador.texto("matricula", matricula, "Matrícula", 3, 20).toUpperCase());
        validador.obrigatorio("orgao", orgao, "Escolha o órgão do gestor.");
        gestor.setOrgao(orgao);

        if (!validador.temErro("email") && usuarioDao.emailEmUso(gestor.getEmail(), id)) {
            validador.erro("email", "Este e-mail já está cadastrado.");
        }
        if (!validador.temErro("matricula") && gestorDao.matriculaEmUso(gestor.getMatricula(), id)) {
            validador.erro("matricula", "Esta matrícula já está cadastrada.");
        }
    }
}
