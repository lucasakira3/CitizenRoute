package br.com.citizenroute.dao;

import br.com.citizenroute.config.OracleConnectionFactory;
import br.com.citizenroute.model.Regiao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acesso à tabela CR_REGIAO. */
public class RegiaoDao implements CrudDao<Regiao> {

    private static final String SQL_INSERIR = "INSERT INTO cr_regiao (nome) VALUES (?)";
    private static final String SQL_LISTAR = "SELECT id_regiao, nome FROM cr_regiao ORDER BY nome";
    private static final String SQL_BUSCAR = "SELECT id_regiao, nome FROM cr_regiao WHERE id_regiao = ?";
    private static final String SQL_ATUALIZAR = "UPDATE cr_regiao SET nome = ? WHERE id_regiao = ?";
    private static final String SQL_EXCLUIR = "DELETE FROM cr_regiao WHERE id_regiao = ?";

    @Override
    public Regiao inserir(Regiao regiao) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR, new String[]{"ID_REGIAO"})) {
            comando.setString(1, regiao.getNome());
            comando.executeUpdate();
            regiao.setId(JdbcUtil.idGerado(comando));
            return regiao;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "cadastrar a região");
        }
    }

    @Override
    public List<Regiao> listar() {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_LISTAR);
             ResultSet resultado = comando.executeQuery()) {
            List<Regiao> regioes = new ArrayList<>();
            while (resultado.next()) {
                regioes.add(mapear(resultado));
            }
            return regioes;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar as regiões");
        }
    }

    @Override
    public Optional<Regiao> buscarPorId(long id) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_BUSCAR)) {
            comando.setLong(1, id);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "buscar a região");
        }
    }

    @Override
    public boolean atualizar(Regiao regiao) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_ATUALIZAR)) {
            comando.setString(1, regiao.getNome());
            comando.setLong(2, regiao.getId());
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "atualizar a região");
        }
    }

    @Override
    public boolean excluir(long id) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_EXCLUIR)) {
            comando.setLong(1, id);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "excluir a região");
        }
    }

    private Regiao mapear(ResultSet resultado) throws SQLException {
        return new Regiao(resultado.getLong("id_regiao"), resultado.getString("nome"));
    }
}
