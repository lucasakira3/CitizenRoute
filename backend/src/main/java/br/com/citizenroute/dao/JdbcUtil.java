package br.com.citizenroute.dao;

import br.com.citizenroute.config.OracleConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;

/** Funções de apoio usadas por todos os DAOs. */
public final class JdbcUtil {

    private JdbcUtil() {
    }

    /** Uma operação que recebe uma conexão aberta e pode lançar SQLException. */
    @FunctionalInterface
    public interface OperacaoJdbc<T> {
        T executar(Connection conexao) throws SQLException;
    }

    /**
     * Executa várias instruções como uma única transação.
     * Se qualquer uma falhar, nada é gravado (rollback). Se todas derem certo, grava tudo (commit).
     */
    public static <T> T emTransacao(String contexto, OperacaoJdbc<T> operacao) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao()) {
            conexao.setAutoCommit(false);
            try {
                T resultado = operacao.executar(conexao);
                conexao.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                conexao.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, contexto);
        }
    }

    /** Lê o id gerado pela coluna IDENTITY depois de um INSERT. */
    public static long idGerado(PreparedStatement comando) throws SQLException {
        try (ResultSet chaves = comando.getGeneratedKeys()) {
            if (!chaves.next()) {
                throw new SQLException("O banco não devolveu o id gerado.");
            }
            return chaves.getLong(1);
        }
    }

    public static LocalDateTime lerData(ResultSet resultado, String coluna) throws SQLException {
        Timestamp valor = resultado.getTimestamp(coluna);
        return valor == null ? null : valor.toLocalDateTime();
    }

    public static Timestamp paraTimestamp(LocalDateTime data) {
        return data == null ? null : Timestamp.valueOf(data);
    }

    /** Monta "?, ?, ?" para cláusulas IN com a quantidade pedida de parâmetros. */
    public static String marcadores(int quantidade) {
        return String.join(", ", Collections.nCopies(quantidade, "?"));
    }
}
