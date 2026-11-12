package br.com.citizenlife.exception;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Campos inválidos (HTTP 400). Também serve para juntar os erros de um formulário:
 * a service cria uma, testa todos os campos e chama {@link #lancarSeHouverErros()} no fim.
 * Assim a tela mostra de uma vez cada erro ao lado do campo certo.
 */
public class ValidacaoException extends CitizenLifeException {

    private final Map<String, String> erros = new LinkedHashMap<>();

    public ValidacaoException() {
        super(400, "Revise os campos destacados.");
    }

    /** Erro de um campo só, lançado na hora. */
    public ValidacaoException(String campo, String mensagem) {
        this();
        erros.put(campo, mensagem);
    }

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

    /** Registra o erro se a condição for falsa. */
    public void exigir(boolean condicao, String campo, String mensagem) {
        if (!condicao) {
            erro(campo, mensagem);
        }
    }

    /** Guarda a primeira mensagem de cada campo. */
    public void erro(String campo, String mensagem) {
        erros.putIfAbsent(campo, mensagem);
    }

    public void lancarSeHouverErros() {
        if (!erros.isEmpty()) {
            throw this;
        }
    }

    /** Com um campo errado, a mensagem é a dele; com mais, um resumo. */
    @Override
    public String getMessage() {
        return erros.size() == 1 ? erros.values().iterator().next() : "Revise os " + erros.size() + " campos destacados.";
    }

    public Map<String, String> getErros() {
        return erros;
    }
}
