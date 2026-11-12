package br.com.citizenlife.exception;

/**
 * O usuário não tem permissão para a operação (HTTP 403 ou 401).
 * Exemplo: um cidadão tentando apagar a rota salva por outra pessoa.
 */
public class AcessoNegadoException extends CitizenLifeException {

    private final boolean naoAutenticado;

    public AcessoNegadoException(String mensagem) {
        this(mensagem, false);
    }

    public AcessoNegadoException(String mensagem, boolean naoAutenticado) {
        super(mensagem);
        this.naoAutenticado = naoAutenticado;
    }

    /** Verdadeiro quando ninguém está identificado (401); falso quando o recurso é de outra pessoa (403). */
    public boolean isNaoAutenticado() {
        return naoAutenticado;
    }
}
