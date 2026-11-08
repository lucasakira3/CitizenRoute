package br.com.citizenroute.exception;

/**
 * O usuário não tem permissão para a operação (HTTP 403 ou 401).
 * Exemplo: um cidadão tentando confirmar uma ocorrência, que é papel do gestor.
 */
public class AcessoNegadoException extends CitizenRouteException {

    private final boolean naoAutenticado;

    public AcessoNegadoException(String mensagem) {
        this(mensagem, false);
    }

    public AcessoNegadoException(String mensagem, boolean naoAutenticado) {
        super(mensagem);
        this.naoAutenticado = naoAutenticado;
    }

    /** Verdadeiro quando ninguém está identificado (401); falso quando o usuário não tem o perfil exigido (403). */
    public boolean isNaoAutenticado() {
        return naoAutenticado;
    }
}
