package br.com.citizenlife.dao;

import br.com.citizenlife.config.OracleConnectionFactory;
import br.com.citizenlife.model.Regiao;
import br.com.citizenlife.model.Via;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Consulta a tabela CL_VIA, sempre trazendo a região junto (JOIN).
 * As vias cadastradas são as que o mapa liga às rotas e às ocorrências.
 */
public class ViaDao {

    private static final String SQL_SELECT = """
            SELECT v.id_via, v.nome, r.id_regiao, r.nome AS nome_regiao
              FROM cl_via v
              JOIN cl_regiao r ON r.id_regiao = v.id_regiao
            """;

    public List<Via> listar() {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_SELECT + " ORDER BY r.nome, v.nome");
             ResultSet resultado = comando.executeQuery()) {
            List<Via> vias = new ArrayList<>();
            while (resultado.next()) {
                vias.add(mapear(resultado));
            }
            return vias;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar as vias");
        }
    }

    public Optional<Via> buscarPorId(long id) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_SELECT + " WHERE v.id_via = ?")) {
            comando.setLong(1, id);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "buscar a via");
        }
    }

    static Via mapear(ResultSet resultado) throws SQLException {
        Regiao regiao = new Regiao(resultado.getLong("id_regiao"), resultado.getString("nome_regiao"));
        return new Via(resultado.getLong("id_via"), resultado.getString("nome"), regiao);
    }
}
