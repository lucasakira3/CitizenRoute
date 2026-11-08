package br.com.citizenroute.dao;

import br.com.citizenroute.config.OracleConnectionFactory;
import br.com.citizenroute.model.Regiao;
import br.com.citizenroute.model.Via;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Acesso à tabela CR_VIA, sempre trazendo a região junto (JOIN). */
public class ViaDao implements CrudDao<Via> {

    private static final String SQL_SELECT = """
            SELECT v.id_via, v.nome, r.id_regiao, r.nome AS nome_regiao
              FROM cr_via v
              JOIN cr_regiao r ON r.id_regiao = v.id_regiao
            """;
    private static final String SQL_INSERIR = "INSERT INTO cr_via (nome, id_regiao) VALUES (?, ?)";
    private static final String SQL_ATUALIZAR = "UPDATE cr_via SET nome = ?, id_regiao = ? WHERE id_via = ?";
    private static final String SQL_EXCLUIR = "DELETE FROM cr_via WHERE id_via = ?";

    @Override
    public Via inserir(Via via) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR, new String[]{"ID_VIA"})) {
            comando.setString(1, via.getNome());
            comando.setLong(2, via.getRegiao().getId());
            comando.executeUpdate();
            via.setId(JdbcUtil.idGerado(comando));
            return via;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "cadastrar a via");
        }
    }

    @Override
    public List<Via> listar() {
        return listar(null, null);
    }

    /**
     * Lista vias com filtros opcionais.
     *
     * @param idRegiao filtra pela região (nulo para todas)
     * @param busca    trecho do nome, sem diferenciar maiúsculas (nulo para todas)
     */
    public List<Via> listar(Long idRegiao, String busca) {
        StringBuilder sql = new StringBuilder(SQL_SELECT).append(" WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (idRegiao != null) {
            sql.append(" AND v.id_regiao = ?");
            parametros.add(idRegiao);
        }
        if (busca != null && !busca.isBlank()) {
            sql.append(" AND UPPER(v.nome) LIKE ?");
            parametros.add("%" + busca.trim().toUpperCase(Locale.ROOT) + "%");
        }
        sql.append(" ORDER BY r.nome, v.nome");

        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                comando.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet resultado = comando.executeQuery()) {
                List<Via> vias = new ArrayList<>();
                while (resultado.next()) {
                    vias.add(mapear(resultado));
                }
                return vias;
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar as vias");
        }
    }

    @Override
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

    @Override
    public boolean atualizar(Via via) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_ATUALIZAR)) {
            comando.setString(1, via.getNome());
            comando.setLong(2, via.getRegiao().getId());
            comando.setLong(3, via.getId());
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "atualizar a via");
        }
    }

    @Override
    public boolean excluir(long id) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_EXCLUIR)) {
            comando.setLong(1, id);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "excluir a via");
        }
    }

    static Via mapear(ResultSet resultado) throws SQLException {
        Regiao regiao = new Regiao(resultado.getLong("id_regiao"), resultado.getString("nome_regiao"));
        return new Via(resultado.getLong("id_via"), resultado.getString("nome"), regiao);
    }
}
