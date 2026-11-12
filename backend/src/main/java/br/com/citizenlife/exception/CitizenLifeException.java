package br.com.citizenlife.exception;

/**
 * Base das exceções da aplicação.
 * É unchecked (RuntimeException) para não poluir cada assinatura com "throws";
 * a camada de interface (api) captura todas em um único lugar e devolve uma mensagem clara.
 */
public abstract class CitizenLifeException extends RuntimeException {

    protected CitizenLifeException(String mensagem) {
        super(mensagem);
    }

    protected CitizenLifeException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
