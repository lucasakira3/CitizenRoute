package br.com.citizenlife.service;

import br.com.citizenlife.dao.CidadaoDao;
import br.com.citizenlife.exception.CitizenLifeException;
import br.com.citizenlife.exception.ValidacaoException;
import br.com.citizenlife.model.Cidadao;
import br.com.citizenlife.model.Ocorrencia;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

/**
 * Cadastro, login e conta do cidadão.
 *
 * <p>Depois do login, o site envia o id do cidadão no cabeçalho X-Usuario-Id; as outras services
 * usam {@link #exigirDono} para garantir que cada um só mexe nos próprios dados.</p>
 * <p>A senha nunca é gravada: o banco guarda só o hash PBKDF2 (pbkdf2$iteracoes$sal$hash).</p>
 */
public class CidadaoService {

    private static final int ITERACOES = 65_536;

    private final CidadaoDao cidadaoDao;

    public CidadaoService(CidadaoDao cidadaoDao) {
        this.cidadaoDao = cidadaoDao;
    }

    /** Cadastro simples: nome, e-mail e senha. O meio de transporte é escolhido depois, no perfil. */
    public Cidadao cadastrar(String nome, String email, String senha) {
        ValidacaoException erros = new ValidacaoException();
        Cidadao cidadao = new Cidadao();
        cidadao.setNome(erros.texto("nome", nome, "Nome", 3, 100));
        cidadao.setEmail(validarEmail(erros, email, null));
        validarSenha(erros, senha);
        erros.lancarSeHouverErros();

        cidadao.setSenhaHash(gerarHash(senha));
        cidadao.setDataCadastro(LocalDateTime.now());
        return cidadaoDao.inserir(cidadao);
    }

    /** Login. A mesma mensagem para e-mail ou senha errados não revela quais e-mails existem. */
    public Cidadao entrar(String email, String senha) {
        String emailLimpo = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return cidadaoDao.buscarPorEmail(emailLimpo)
                .filter(cidadao -> senhaConfere(senha, cidadao.getSenhaHash()))
                .orElseThrow(() -> CitizenLifeException.naoAutenticado("E-mail ou senha incorretos."));
    }

    /** Garante que quem pede (idAtor) é o dono da conta idCidadao. */
    public Cidadao exigirDono(Long idAtor, long idCidadao) {
        if (idAtor == null) {
            throw CitizenLifeException.naoAutenticado("Entre no Citizen Life para continuar.");
        }
        Cidadao cidadao = cidadaoDao.buscarPorId(idAtor)
                .orElseThrow(() -> CitizenLifeException.naoAutenticado("Sua sessão expirou. Entre novamente."));
        if (cidadao.getId() != idCidadao) {
            throw CitizenLifeException.acessoNegado("Você só pode mexer na sua própria conta.");
        }
        return cidadao;
    }

    /** Atualiza nome, e-mail e meio de transporte (opcional). A senha só muda quando uma nova é informada. */
    public Cidadao atualizar(long id, String nome, String email, String modal, String novaSenha, Long idAtor) {
        Cidadao cidadao = exigirDono(idAtor, id);
        ValidacaoException erros = new ValidacaoException();
        cidadao.setNome(erros.texto("nome", nome, "Nome", 3, 100));
        cidadao.setEmail(validarEmail(erros, email, id));
        erros.exigir(modal == null || Ocorrencia.MODAIS.contains(modal), "modal", "Meio de transporte inválido.");
        boolean trocarSenha = novaSenha != null && !novaSenha.isEmpty();
        if (trocarSenha) {
            validarSenha(erros, novaSenha);
        }
        erros.lancarSeHouverErros();

        cidadao.setModal(modal);
        if (trocarSenha) {
            cidadao.setSenhaHash(gerarHash(novaSenha));
        }
        cidadaoDao.atualizar(cidadao);
        return cidadao;
    }

    /** Exclui a conta; as rotas e os endereços vão junto. */
    public void excluir(long id, Long idAtor) {
        exigirDono(idAtor, id);
        cidadaoDao.excluir(id);
    }

    private String validarEmail(ValidacaoException erros, String email, Long idDono) {
        String limpo = erros.texto("email", email, "E-mail", 6, 120).toLowerCase(Locale.ROOT);
        if (!erros.getErros().containsKey("email")) {
            erros.exigir(limpo.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$"), "email", "Informe um e-mail válido.");
            boolean usado = cidadaoDao.buscarPorEmail(limpo).filter(outro -> !outro.getId().equals(idDono)).isPresent();
            erros.exigir(!usado, "email", "Este e-mail já está cadastrado.");
        }
        return limpo;
    }

    private void validarSenha(ValidacaoException erros, String senha) {
        boolean valida = senha != null && senha.length() >= 8 && senha.length() <= 64
                && senha.matches(".*[A-Za-z].*") && senha.matches(".*\\d.*");
        erros.exigir(valida, "senha", "A senha precisa ter de 8 a 64 caracteres, com letras e números.");
    }

    private String gerarHash(String senha) {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        Base64.Encoder base64 = Base64.getEncoder();
        return "pbkdf2$" + ITERACOES + "$" + base64.encodeToString(sal) + "$"
                + base64.encodeToString(derivar(senha, sal, ITERACOES));
    }

    private boolean senhaConfere(String senha, String hash) {
        String[] partes = hash == null ? new String[0] : hash.split("\\$");
        if (senha == null || partes.length != 4) {
            return false;
        }
        byte[] sal = Base64.getDecoder().decode(partes[2]);
        byte[] esperado = Base64.getDecoder().decode(partes[3]);
        // Comparação em tempo constante: não revela quantos bytes coincidiram
        return MessageDigest.isEqual(esperado, derivar(senha, sal, Integer.parseInt(partes[1])));
    }

    private byte[] derivar(String senha, byte[] sal, int iteracoes) {
        try {
            PBEKeySpec chave = new PBEKeySpec(senha.toCharArray(), sal, iteracoes, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(chave).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponível nesta JVM.", e);
        }
    }
}
