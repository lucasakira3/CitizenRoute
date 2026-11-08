package br.com.citizenroute.exception;

/**
 * A entrada é válida, mas a operação viola uma regra do domínio (HTTP 409).
 * Exemplo: tentar reabrir uma ocorrência que já foi resolvida.
 */
public class RegraNegocioException extends CitizenRouteException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
