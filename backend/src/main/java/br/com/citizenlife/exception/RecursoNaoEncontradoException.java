package br.com.citizenlife.exception;

/** O registro pedido não existe no banco (HTTP 404). */
public class RecursoNaoEncontradoException extends CitizenLifeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
