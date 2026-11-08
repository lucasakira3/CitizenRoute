package br.com.citizenroute.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Gera e confere hashes de senha com PBKDF2, que já vem no JDK.
 * O banco guarda só o hash, no formato pbkdf2$iteracoes$sal$hash, nunca a senha.
 */
public class SenhaService {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 65_536;
    private static final int TAMANHO_SAL = 16;
    private static final int TAMANHO_HASH_BITS = 256;

    private final SecureRandom aleatorio = new SecureRandom();

    public String gerarHash(String senha) {
        byte[] sal = new byte[TAMANHO_SAL];
        aleatorio.nextBytes(sal);
        byte[] hash = derivar(senha, sal, ITERACOES);
        Base64.Encoder base64 = Base64.getEncoder();
        return "pbkdf2$" + ITERACOES + "$" + base64.encodeToString(sal) + "$" + base64.encodeToString(hash);
    }

    public boolean confere(String senha, String hashArmazenado) {
        if (senha == null || hashArmazenado == null) {
            return false;
        }
        String[] partes = hashArmazenado.split("\\$");
        if (partes.length != 4 || !"pbkdf2".equals(partes[0])) {
            return false;
        }
        try {
            int iteracoes = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            // Comparação em tempo constante: não revela quantos bytes coincidiram.
            return MessageDigest.isEqual(esperado, derivar(senha, sal, iteracoes));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] derivar(String senha, byte[] sal, int iteracoes) {
        PBEKeySpec especificacao = new PBEKeySpec(senha.toCharArray(), sal, iteracoes, TAMANHO_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacao).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponível nesta JVM.", e);
        } finally {
            especificacao.clearPassword();
        }
    }
}
