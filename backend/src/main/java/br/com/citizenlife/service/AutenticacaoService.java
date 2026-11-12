package br.com.citizenlife.service;

import br.com.citizenlife.dao.CidadaoDao;
import br.com.citizenlife.exception.AcessoNegadoException;
import br.com.citizenlife.model.Cidadao;

/**
 * Login e verificação de quem está fazendo a operação.
 *
 * <p>No MVP, o front-end guarda o id do cidadão depois do login e o envia no cabeçalho X-Usuario-Id.
 * Cada operação confere aqui se esse cidadão existe e se pode mexer no recurso pedido.
 * Uma versão de produção trocaria o id por um token assinado (JWT ou sessão no servidor).</p>
 */
public class AutenticacaoService {

    private static final String MENSAGEM_LOGIN_INVALIDO = "E-mail ou senha incorretos.";

    private final CidadaoDao cidadaoDao;
    private final SenhaService senhaService;

    public AutenticacaoService(CidadaoDao cidadaoDao, SenhaService senhaService) {
        this.cidadaoDao = cidadaoDao;
        this.senhaService = senhaService;
    }

    public Cidadao entrar(String email, String senha) {
        Validador validador = new Validador();
        String emailLimpo = validador.email("email", email);
        validador.exigir(senha != null && !senha.isEmpty(), "senha", "Preencha o campo senha.");
        validador.validar();

        // A mesma mensagem para e-mail inexistente e senha errada não revela quais e-mails existem.
        Cidadao cidadao = cidadaoDao.buscarPorEmail(emailLimpo)
                .orElseThrow(() -> new AcessoNegadoException(MENSAGEM_LOGIN_INVALIDO, true));
        if (!senhaService.confere(senha, cidadao.getSenhaHash())) {
            throw new AcessoNegadoException(MENSAGEM_LOGIN_INVALIDO, true);
        }
        return cidadao;
    }

    /** Garante que há um cidadão identificado e que ele ainda existe no banco. */
    public Cidadao exigirCidadao(Long idUsuario) {
        if (idUsuario == null) {
            throw new AcessoNegadoException("Entre no Citizen Life para continuar.", true);
        }
        return cidadaoDao.buscarPorId(idUsuario)
                .orElseThrow(() -> new AcessoNegadoException("Sua sessão não é mais válida. Entre novamente.", true));
    }

    /**
     * Garante que quem faz a operação é o dono da conta.
     *
     * @param acao o que se quer fazer, para compor a mensagem: "ver as rotas"
     */
    public Cidadao exigirProprio(Long idUsuario, long idAlvo, String acao) {
        Cidadao cidadao = exigirCidadao(idUsuario);
        if (cidadao.getId() != idAlvo) {
            throw new AcessoNegadoException("Você só pode " + acao + " da sua própria conta.");
        }
        return cidadao;
    }
}
