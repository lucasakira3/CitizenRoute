package br.com.citizenlife.service;

import br.com.citizenlife.exception.ValidacaoException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Junta todos os erros de um formulário antes de responder.
 * Assim o usuário vê de uma vez tudo o que precisa corrigir, e não um erro por tentativa.
 */
public class Validador {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final Map<String, String> erros = new LinkedHashMap<>();

    /** Valida um texto obrigatório e devolve o valor sem espaços nas pontas. */
    public String texto(String campo, String valor, String rotulo, int minimo, int maximo) {
        String limpo = valor == null ? "" : valor.trim();
        if (limpo.isEmpty()) {
            erro(campo, "Preencha o campo " + rotulo.toLowerCase(Locale.ROOT) + ".");
        } else if (limpo.length() < minimo) {
            erro(campo, rotulo + " precisa ter pelo menos " + minimo + " caracteres.");
        } else if (limpo.length() > maximo) {
            erro(campo, rotulo + " pode ter no máximo " + maximo + " caracteres.");
        }
        return limpo;
    }

    /** Valida o formato do e-mail e devolve o valor em minúsculas. */
    public String email(String campo, String valor) {
        String limpo = texto(campo, valor, "E-mail", 6, 120).toLowerCase(Locale.ROOT);
        if (!erros.containsKey(campo) && !EMAIL.matcher(limpo).matches()) {
            erro(campo, "Informe um e-mail válido, como nome@exemplo.com.");
        }
        return limpo;
    }

    /** Senha com pelo menos 8 caracteres, contendo letra e número. */
    public void senha(String campo, String valor) {
        if (valor == null || valor.isEmpty()) {
            erro(campo, "Preencha o campo senha.");
        } else if (valor.length() < 8 || valor.length() > 64) {
            erro(campo, "A senha precisa ter entre 8 e 64 caracteres.");
        } else if (!valor.matches(".*[A-Za-z].*") || !valor.matches(".*\\d.*")) {
            erro(campo, "A senha precisa ter letras e números.");
        }
    }

    public void intervalo(String campo, Integer valor, String rotulo, int minimo, int maximo) {
        if (valor == null) {
            erro(campo, "Preencha o campo " + rotulo.toLowerCase(Locale.ROOT) + ".");
        } else if (valor < minimo || valor > maximo) {
            erro(campo, rotulo + " deve estar entre " + minimo + " e " + maximo + ".");
        }
    }

    public void obrigatorio(String campo, Object valor, String mensagem) {
        if (valor == null) {
            erro(campo, mensagem);
        }
    }

    public void exigir(boolean condicao, String campo, String mensagem) {
        if (!condicao) {
            erro(campo, mensagem);
        }
    }

    /** Registra o erro do campo, mantendo a primeira mensagem quando houver mais de uma. */
    public void erro(String campo, String mensagem) {
        erros.putIfAbsent(campo, mensagem);
    }

    public boolean temErro(String campo) {
        return erros.containsKey(campo);
    }

    /** Lança ValidacaoException se algum campo tiver erro. */
    public void validar() {
        if (!erros.isEmpty()) {
            String mensagem = erros.size() == 1
                    ? erros.values().iterator().next()
                    : "Revise os " + erros.size() + " campos destacados.";
            throw new ValidacaoException(mensagem, erros);
        }
    }
}
