package br.com.citizenlife.exception;

/**
 * Falha ao acessar o banco de dados.
 * Os DAOs convertem toda SQLException nesta exceção, já com uma mensagem legível.
 */
public class PersistenciaException extends CitizenLifeException {

    private final boolean bancoIndisponivel;
    private final boolean conflitoDeDados;

    public PersistenciaException(String mensagem, Throwable causa) {
        this(mensagem, causa, false, false);
    }

    public PersistenciaException(String mensagem, Throwable causa, boolean bancoIndisponivel, boolean conflitoDeDados) {
        super(mensagem, causa);
        this.bancoIndisponivel = bancoIndisponivel;
        this.conflitoDeDados = conflitoDeDados;
    }

    public static PersistenciaException indisponivel(String mensagem, Throwable causa) {
        return new PersistenciaException(mensagem, causa, true, false);
    }

    public static PersistenciaException conflito(String mensagem, Throwable causa) {
        return new PersistenciaException(mensagem, causa, false, true);
    }

    /** Banco fora do ar, credenciais erradas ou tabelas ausentes (HTTP 503). */
    public boolean isBancoIndisponivel() {
        return bancoIndisponivel;
    }

    /** Restrição do banco violada, como e-mail repetido ou exclusão com registros vinculados (HTTP 409). */
    public boolean isConflitoDeDados() {
        return conflitoDeDados;
    }
}
