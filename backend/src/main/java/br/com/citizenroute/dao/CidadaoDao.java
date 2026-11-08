package br.com.citizenroute.dao;

import br.com.citizenroute.config.OracleConnectionFactory;
import br.com.citizenroute.model.Cidadao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acesso às tabelas CR_USUARIO e CR_CIDADAO.
 * Como o cidadão ocupa uma linha em cada tabela, inserir e atualizar usam transação.
 */
public class CidadaoDao implements CrudDao<Cidadao> {

    private static final String SQL_SELECT = """
            SELECT u.id_usuario, u.nome, u.email, u.senha_hash, u.tipo, u.dt_cadastro,
                   c.modal, NULL AS orgao, NULL AS matricula
              FROM cr_cidadao c
              JOIN cr_usuario u ON u.id_usuario = c.id_usuario
            """;
    private static final String SQL_INSERIR_USUARIO =
            "INSERT INTO cr_usuario (nome, email, senha_hash, tipo, dt_cadastro) VALUES (?, ?, ?, 'CIDADAO', ?)";
    private static final String SQL_INSERIR_CIDADAO = "INSERT INTO cr_cidadao (id_usuario, modal) VALUES (?, ?)";
    private static final String SQL_ATUALIZAR_USUARIO =
            "UPDATE cr_usuario SET nome = ?, email = ?, senha_hash = ? WHERE id_usuario = ? AND tipo = 'CIDADAO'";
    private static final String SQL_ATUALIZAR_CIDADAO = "UPDATE cr_cidadao SET modal = ? WHERE id_usuario = ?";
    // ON DELETE CASCADE remove a linha de CR_CIDADAO e as vias monitoradas junto com o usuário.
    private static final String SQL_EXCLUIR = "DELETE FROM cr_usuario WHERE id_usuario = ? AND tipo = 'CIDADAO'";

    @Override
    public Cidadao inserir(Cidadao cidadao) {
        return JdbcUtil.emTransacao("cadastrar o cidadão", conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR_USUARIO, new String[]{"ID_USUARIO"})) {
                comando.setString(1, cidadao.getNome());
                comando.setString(2, cidadao.getEmail());
                comando.setString(3, cidadao.getSenhaHash());
                comando.setTimestamp(4, JdbcUtil.paraTimestamp(cidadao.getDataCadastro()));
                comando.executeUpdate();
                cidadao.setId(JdbcUtil.idGerado(comando));
            }
            try (PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR_CIDADAO)) {
                comando.setLong(1, cidadao.getId());
                comando.setString(2, cidadao.getModal().name());
                comando.executeUpdate();
            }
            return cidadao;
        });
    }

    @Override
    public List<Cidadao> listar() {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_SELECT + " ORDER BY u.nome");
             ResultSet resultado = comando.executeQuery()) {
            List<Cidadao> cidadaos = new ArrayList<>();
            while (resultado.next()) {
                cidadaos.add((Cidadao) UsuarioDao.mapear(resultado));
            }
            return cidadaos;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar os cidadãos");
        }
    }

    @Override
    public Optional<Cidadao> buscarPorId(long id) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_SELECT + " WHERE c.id_usuario = ?")) {
            comando.setLong(1, id);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next() ? Optional.of((Cidadao) UsuarioDao.mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "buscar o cidadão");
        }
    }

    @Override
    public boolean atualizar(Cidadao cidadao) {
        return JdbcUtil.emTransacao("atualizar o cidadão", conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(SQL_ATUALIZAR_USUARIO)) {
                comando.setString(1, cidadao.getNome());
                comando.setString(2, cidadao.getEmail());
                comando.setString(3, cidadao.getSenhaHash());
                comando.setLong(4, cidadao.getId());
                if (comando.executeUpdate() == 0) {
                    return false;
                }
            }
            try (PreparedStatement comando = conexao.prepareStatement(SQL_ATUALIZAR_CIDADAO)) {
                comando.setString(1, cidadao.getModal().name());
                comando.setLong(2, cidadao.getId());
                comando.executeUpdate();
            }
            return true;
        });
    }

    @Override
    public boolean excluir(long id) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_EXCLUIR)) {
            comando.setLong(1, id);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "excluir o cidadão");
        }
    }
}
