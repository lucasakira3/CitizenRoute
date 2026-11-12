package br.com.citizenlife.exception;

/**
 * Erro da aplicação com o código HTTP que a API deve responder.
 * O Servidor captura todas em um único lugar e devolve a mensagem para a tela, sem derrubar nada.
 */
public class CitizenLifeException extends RuntimeException {

    private final int status;

    public CitizenLifeException(int status, String mensagem) {
        this(status, mensagem, null);
    }

    public CitizenLifeException(int status, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.status = status;
    }

    /** 404: o registro não existe (ou não é do cidadão que pediu). */
    public static CitizenLifeException naoEncontrado(String mensagem) {
        return new CitizenLifeException(404, mensagem);
    }

    /** 401: ninguém entrou, ou o login está errado. */
    public static CitizenLifeException naoAutenticado(String mensagem) {
        return new CitizenLifeException(401, mensagem);
    }

    /** 403: entrou, mas o recurso é de outra pessoa. */
    public static CitizenLifeException acessoNegado(String mensagem) {
        return new CitizenLifeException(403, mensagem);
    }

    /** 409: o banco recusou (registro repetido, por exemplo). */
    public static CitizenLifeException conflito(String mensagem, Throwable causa) {
        return new CitizenLifeException(409, mensagem, causa);
    }

    /** 503: o banco não respondeu ou não está configurado. */
    public static CitizenLifeException bancoIndisponivel(String mensagem, Throwable causa) {
        return new CitizenLifeException(503, mensagem, causa);
    }

    public int getStatus() {
        return status;
    }
}
