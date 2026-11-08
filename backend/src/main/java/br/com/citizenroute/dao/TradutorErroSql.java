package br.com.citizenroute.dao;

import br.com.citizenroute.exception.PersistenciaException;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

/**
 * Converte SQLException em PersistenciaException com mensagem que o usuário entende.
 *
 * <p>Usa o código de erro do Oracle (ORA-xxxxx) e o nome da constraint violada.
 * Por isso as constraints do script.sql têm nomes explícitos, como UK_CR_USUARIO_EMAIL.</p>
 */
public final class TradutorErroSql {

    // Códigos de erro do Oracle
    private static final int ORA_UNIQUE = 1;
    private static final int ORA_NULL = 1400;
    private static final int ORA_TABELA_INEXISTENTE = 942;
    private static final int ORA_CHECK = 2290;
    private static final int ORA_PAI_INEXISTENTE = 2291;
    private static final int ORA_FILHOS_EXISTEM = 2292;
    private static final int ORA_VALOR_GRANDE = 12899;
    private static final int ORA_LOGIN_INVALIDO = 1017;
    private static final int ORA_CONTA_BLOQUEADA = 28000;
    private static final int ORA_SENHA_EXPIRADA = 28001;

    /** Mensagens para cada constraint UNIQUE ou PK que o usuário pode violar. */
    private static final Map<String, String> UNICIDADE = Map.of(
            "UK_CR_USUARIO_EMAIL", "Este e-mail já está cadastrado.",
            "UK_CR_GESTOR_MATRICULA", "Esta matrícula já está cadastrada.",
            "UK_CR_REGIAO_NOME", "Já existe uma região com este nome.",
            "UK_CR_VIA_NOME_REGIAO", "Já existe uma via com este nome nesta região.",
            "PK_CR_VIA_MONITORADA", "Esta via já está na sua lista de vias monitoradas.",
            "PK_CR_OCORRENCIA_VIA", "A mesma via foi informada mais de uma vez na ocorrência.");

    /** Mensagens para exclusões bloqueadas porque ainda existem registros filhos. */
    private static final Map<String, String> EXCLUSAO_BLOQUEADA = Map.of(
            "FK_CR_VIA_REGIAO", "Não é possível excluir a região: existem vias cadastradas nela.",
            "FK_CR_OCVIA_VIA", "Não é possível excluir a via: existem ocorrências registradas nela.",
            "FK_CR_OCORRENCIA_CIDADAO", "Não é possível excluir o cidadão: ele é autor de ocorrências registradas.",
            "FK_CR_ACAO_GESTOR", "Não é possível excluir o gestor: existem ações registradas por ele.");

    private TradutorErroSql() {
    }

    /** Traduz erros de leitura e gravação. */
    public static PersistenciaException traduzir(SQLException e, String contexto) {
        int codigo = e.getErrorCode();
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        String mensagem = e.getMessage() == null ? "" : e.getMessage().toUpperCase(Locale.ROOT);

        if (codigo == ORA_UNIQUE || "23505".equals(estado)) {
            return PersistenciaException.conflito(
                    procurar(UNICIDADE, mensagem, "Já existe um registro com estes dados."), e);
        }
        if (codigo == ORA_FILHOS_EXISTEM || codigo == 23503) {
            return PersistenciaException.conflito(
                    procurar(EXCLUSAO_BLOQUEADA, mensagem, "Não é possível excluir: existem registros vinculados."), e);
        }
        if (codigo == ORA_PAI_INEXISTENTE || codigo == 23506) {
            return PersistenciaException.conflito(
                    "Um registro relacionado não existe mais. Atualize a página e tente de novo.", e);
        }
        if (codigo == ORA_CHECK || "23513".equals(estado)) {
            return PersistenciaException.conflito("Um dos valores está fora das opções permitidas.", e);
        }
        if (codigo == ORA_NULL || "23502".equals(estado)) {
            return PersistenciaException.conflito("Um campo obrigatório não foi informado.", e);
        }
        if (codigo == ORA_VALOR_GRANDE || "22001".equals(estado)) {
            return PersistenciaException.conflito("Um dos textos passou do tamanho máximo permitido.", e);
        }
        if (codigo == ORA_TABELA_INEXISTENTE || "42S02".equals(estado) || codigo == 42102 || codigo == 42104) {
            return PersistenciaException.indisponivel(
                    "As tabelas do CitizenRoute não foram encontradas. Execute sql/script.sql no seu usuário Oracle.", e);
        }
        if (estado.startsWith("08")) {
            return traduzirConexao(e);
        }
        return new PersistenciaException("Erro no banco de dados ao " + contexto + ".", e);
    }

    /** Traduz erros ao abrir a conexão. */
    public static PersistenciaException traduzirConexao(SQLException e) {
        int codigo = e.getErrorCode();
        String detalhe;
        if (codigo == ORA_LOGIN_INVALIDO) {
            detalhe = "usuário ou senha do Oracle inválidos. Confira ORACLE_DB_USER e ORACLE_DB_PASSWORD.";
        } else if (codigo == ORA_CONTA_BLOQUEADA) {
            detalhe = "a conta do Oracle está bloqueada.";
        } else if (codigo == ORA_SENHA_EXPIRADA) {
            detalhe = "a senha do Oracle expirou. Troque a senha e atualize a configuração.";
        } else if (e.getMessage() != null && e.getMessage().contains("No suitable driver")) {
            detalhe = "a URL não é uma URL JDBC válida. Exemplo: jdbc:oracle:thin:@//host:1521/servico.";
        } else {
            detalhe = "o servidor não respondeu. Confira ORACLE_JDBC_URL e a sua conexão de rede.";
        }
        return PersistenciaException.indisponivel("Não foi possível conectar ao Oracle: " + detalhe, e);
    }

    private static String procurar(Map<String, String> mensagens, String erro, String padrao) {
        String melhor = null;
        for (String constraint : mensagens.keySet()) {
            boolean citada = erro.contains(constraint);
            if (citada && (melhor == null || constraint.length() > melhor.length())) {
                melhor = constraint;
            }
        }
        return melhor == null ? padrao : mensagens.get(melhor);
    }
}
