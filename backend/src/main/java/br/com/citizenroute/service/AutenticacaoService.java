package br.com.citizenroute.service;

import br.com.citizenroute.dao.UsuarioDao;
import br.com.citizenroute.exception.AcessoNegadoException;
import br.com.citizenroute.model.Cidadao;
import br.com.citizenroute.model.Gestor;
import br.com.citizenroute.model.Usuario;

/**
 * Login e verificação de perfil.
 *
 * <p>No MVP, o front-end guarda o id do usuário depois do login e o envia no cabeçalho X-Usuario-Id.
 * Cada operação confere aqui se esse usuário existe e se tem o perfil exigido.
 * Uma versão de produção trocaria o id por um token assinado (JWT ou sessão no servidor).</p>
 */
public class AutenticacaoService {

    private static final String MENSAGEM_LOGIN_INVALIDO = "E-mail ou senha incorretos.";

    private final UsuarioDao usuarioDao;
    private final SenhaService senhaService;

    public AutenticacaoService(UsuarioDao usuarioDao, SenhaService senhaService) {
        this.usuarioDao = usuarioDao;
        this.senhaService = senhaService;
    }

    public Usuario entrar(String email, String senha) {
        Validador validador = new Validador();
        String emailLimpo = validador.email("email", email);
        validador.exigir(senha != null && !senha.isEmpty(), "senha", "Preencha o campo senha.");
        validador.validar();

        // A mesma mensagem para e-mail inexistente e senha errada não revela quais e-mails existem.
        Usuario usuario = usuarioDao.buscarPorEmail(emailLimpo)
                .orElseThrow(() -> new AcessoNegadoException(MENSAGEM_LOGIN_INVALIDO, true));
        if (!senhaService.confere(senha, usuario.getSenhaHash())) {
            throw new AcessoNegadoException(MENSAGEM_LOGIN_INVALIDO, true);
        }
        return usuario;
    }

    /** Garante que há um usuário identificado e que ele ainda existe no banco. */
    public Usuario exigirUsuario(Long idUsuario) {
        if (idUsuario == null) {
            throw new AcessoNegadoException("Entre no sistema para continuar.", true);
        }
        return usuarioDao.buscarPorId(idUsuario)
                .orElseThrow(() -> new AcessoNegadoException("Sua sessão não é mais válida. Entre novamente.", true));
    }

    /**
     * Garante que o usuário é gestor.
     *
     * @param acao o que se quer fazer, para compor a mensagem: "confirmar ocorrências"
     */
    public Gestor exigirGestor(Long idUsuario, String acao) {
        Usuario usuario = exigirUsuario(idUsuario);
        if (usuario instanceof Gestor gestor) {
            return gestor;
        }
        throw new AcessoNegadoException("Apenas gestores públicos podem " + acao + ".");
    }

    /** Garante que o usuário é cidadão. */
    public Cidadao exigirCidadao(Long idUsuario, String acao) {
        Usuario usuario = exigirUsuario(idUsuario);
        if (usuario instanceof Cidadao cidadao) {
            return cidadao;
        }
        throw new AcessoNegadoException("Apenas cidadãos podem " + acao + ".");
    }

    /** Permite a operação ao próprio usuário ou a qualquer gestor. */
    public Usuario exigirProprioOuGestor(Long idUsuario, long idAlvo, String acao) {
        Usuario usuario = exigirUsuario(idUsuario);
        if (usuario instanceof Gestor || usuario.getId() == idAlvo) {
            return usuario;
        }
        throw new AcessoNegadoException("Você só pode " + acao + " da sua própria conta.");
    }
}
