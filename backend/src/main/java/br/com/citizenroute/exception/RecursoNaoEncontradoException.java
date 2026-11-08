package br.com.citizenroute.exception;

/** O registro pedido não existe no banco (HTTP 404). */
public class RecursoNaoEncontradoException extends CitizenRouteException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
