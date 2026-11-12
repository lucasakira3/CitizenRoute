package br.com.citizenlife.dao;

import br.com.citizenlife.config.OracleConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Consulta mínima para saber se o banco responde e se as tabelas do projeto existem. */
public class SaudeDao {

    private static final String SQL_VERIFICAR = "SELECT COUNT(*) FROM cl_cidadao";

    /** Lança PersistenciaException com a causa, já traduzida, quando algo estiver errado. */
    public void verificar() {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_VERIFICAR);
             ResultSet resultado = comando.executeQuery()) {
            resultado.next();
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "verificar o banco");
        }
    }

    public String origemConfiguracao() {
        return OracleConnectionFactory.descreverOrigem();
    }
}
