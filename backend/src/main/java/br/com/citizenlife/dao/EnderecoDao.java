package br.com.citizenlife.dao;

import br.com.citizenlife.config.OracleConnectionFactory;
import br.com.citizenlife.model.Endereco;
import br.com.citizenlife.model.PontoRota;
import br.com.citizenlife.model.TipoEndereco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acesso à tabela CL_ENDERECO: os endereços salvos de cada cidadão (casa, trabalho...). */
public class EnderecoDao implements CrudDao<Endereco> {

    private static final String SQL_SELECT =
            "SELECT id_endereco, id_cidadao, apelido, tipo, descricao, pos_x, pos_y, dt_criacao FROM cl_endereco";
    // Casa e trabalho primeiro, depois pelo apelido
    private static final String ORDEM = """
             ORDER BY CASE tipo WHEN 'CASA' THEN 1 WHEN 'TRABALHO' THEN 2 WHEN 'ESTUDO' THEN 3 ELSE 4 END, apelido
            """;
    private static final String SQL_INSERIR = """
            INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y, dt_criacao)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String SQL_ATUALIZAR = """
            UPDATE cl_endereco
               SET apelido = ?, tipo = ?, descricao = ?, pos_x = ?, pos_y = ?
             WHERE id_endereco = ?
            """;
    private static final String SQL_EXCLUIR = "DELETE FROM cl_endereco WHERE id_endereco = ?";

    @Override
    public Endereco inserir(Endereco endereco) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR, new String[]{"ID_ENDERECO"})) {
            comando.setLong(1, endereco.getIdCidadao());
            comando.setString(2, endereco.getApelido());
            comando.setString(3, endereco.getTipo().name());
            comando.setString(4, endereco.getLocal().descricao());
            comando.setDouble(5, endereco.getLocal().x());
            comando.setDouble(6, endereco.getLocal().y());
            comando.setTimestamp(7, JdbcUtil.paraTimestamp(endereco.getDataCriacao()));
            comando.executeUpdate();
            endereco.setId(JdbcUtil.idGerado(comando));
            return endereco;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "salvar o endereço");
        }
    }

    @Override
    public List<Endereco> listar() {
        return consultar(SQL_SELECT + ORDEM, null);
    }

    /** Endereços de um cidadão: casa e trabalho primeiro. */
    public List<Endereco> listarPorCidadao(long idCidadao) {
        return consultar(SQL_SELECT + " WHERE id_cidadao = ?" + ORDEM, idCidadao);
    }

    @Override
    public Optional<Endereco> buscarPorId(long id) {
        return consultar(SQL_SELECT + " WHERE id_endereco = ?", id).stream().findFirst();
    }

    @Override
    public boolean atualizar(Endereco endereco) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_ATUALIZAR)) {
            comando.setString(1, endereco.getApelido());
            comando.setString(2, endereco.getTipo().name());
            comando.setString(3, endereco.getLocal().descricao());
            comando.setDouble(4, endereco.getLocal().x());
            comando.setDouble(5, endereco.getLocal().y());
            comando.setLong(6, endereco.getId());
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "atualizar o endereço");
        }
    }

    @Override
    public boolean excluir(long id) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_EXCLUIR)) {
            comando.setLong(1, id);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "excluir o endereço");
        }
    }

    private List<Endereco> consultar(String sql, Long parametro) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            if (parametro != null) {
                comando.setLong(1, parametro);
            }
            try (ResultSet resultado = comando.executeQuery()) {
                List<Endereco> enderecos = new ArrayList<>();
                while (resultado.next()) {
                    enderecos.add(mapear(resultado));
                }
                return enderecos;
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar os endereços");
        }
    }

    private Endereco mapear(ResultSet resultado) throws SQLException {
        PontoRota local = new PontoRota(resultado.getString("descricao"), resultado.getDouble("pos_x"),
                resultado.getDouble("pos_y"));
        Endereco endereco = new Endereco(resultado.getLong("id_endereco"), resultado.getLong("id_cidadao"),
                resultado.getString("apelido"), TipoEndereco.valueOf(resultado.getString("tipo")), local);
        endereco.setDataCriacao(JdbcUtil.lerData(resultado, "dt_criacao"));
        return endereco;
    }
}
