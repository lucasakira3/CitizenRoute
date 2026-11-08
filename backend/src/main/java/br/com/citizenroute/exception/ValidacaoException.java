package br.com.citizenroute.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entrada inválida enviada pelo usuário (HTTP 400).
 * Guarda os erros por campo para o front-end mostrar cada mensagem ao lado do campo certo.
 */
public class ValidacaoException extends CitizenRouteException {

    private final Map<String, String> erros;

    public ValidacaoException(String mensagem) {
        this(mensagem, Map.of());
    }

    public ValidacaoException(String mensagem, Map<String, String> erros) {
        super(mensagem);
        this.erros = Collections.unmodifiableMap(new LinkedHashMap<>(erros));
    }

    public Map<String, String> getErros() {
        return erros;
    }
}
