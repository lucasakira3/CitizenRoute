package br.com.citizenroute.exception;

/**
 * Base das exceções da aplicação.
 * É unchecked (RuntimeException) para não poluir cada assinatura com "throws";
 * a camada de interface (api) captura todas em um único lugar e devolve uma mensagem clara.
 */
public abstract class CitizenRouteException extends RuntimeException {

    protected CitizenRouteException(String mensagem) {
        super(mensagem);
    }

    protected CitizenRouteException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
