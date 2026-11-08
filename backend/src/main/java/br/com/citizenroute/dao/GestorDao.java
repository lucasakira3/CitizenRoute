package br.com.citizenroute.dao;

import br.com.citizenroute.config.OracleConnectionFactory;
import br.com.citizenroute.model.Gestor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acesso às tabelas CR_USUARIO e CR_GESTOR.
 * Como o gestor ocupa uma linha em cada tabela, inserir e atualizar usam transação.
 */
public class GestorDao implements CrudDao<Gestor> {

    private static final String SQL_SELECT = """
            SELECT u.id_usuario, u.nome, u.email, u.senha_hash, u.tipo, u.dt_cadastro,
                   NULL AS modal, g.orgao, g.matricula
              FROM cr_gestor g
              JOIN cr_usuario u ON u.id_usuario = g.id_usuario
            """;
    private static final String SQL_INSERIR_USUARIO =
            "INSERT INTO cr_usuario (nome, email, senha_hash, tipo, dt_cadastro) VALUES (?, ?, ?, 'GESTOR', ?)";
    private static final String SQL_INSERIR_GESTOR =
            "INSERT INTO cr_gestor (id_usuario, orgao, matricula) VALUES (?, ?, ?)";
    private static final String SQL_ATUALIZAR_USUARIO =
            "UPDATE cr_usuario SET nome = ?, email = ?, senha_hash = ? WHERE id_usuario = ? AND tipo = 'GESTOR'";
    private static final String SQL_ATUALIZAR_GESTOR =
            "UPDATE cr_gestor SET orgao = ?, matricula = ? WHERE id_usuario = ?";
    private static final String SQL_EXCLUIR = "DELETE FROM cr_usuario WHERE id_usuario = ? AND tipo = 'GESTOR'";
    private static final String SQL_MATRICULA_EM_USO =
            "SELECT COUNT(*) FROM cr_gestor WHERE matricula = ? AND id_usuario <> ?";

    @Override
    public Gestor inserir(Gestor gestor) {
        return JdbcUtil.emTransacao("cadastrar o gestor", conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR_USUARIO, new String[]{"ID_USUARIO"})) {
                comando.setString(1, gestor.getNome());
                comando.setString(2, gestor.getEmail());
                comando.setString(3, gestor.getSenhaHash());
                comando.setTimestamp(4, JdbcUtil.paraTimestamp(gestor.getDataCadastro()));
                comando.executeUpdate();
                gestor.setId(JdbcUtil.idGerado(comando));
            }
            try (PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR_GESTOR)) {
                comando.setLong(1, gestor.getId());
                comando.setString(2, gestor.getOrgao().name());
                comando.setString(3, gestor.getMatricula());
                comando.executeUpdate();
            }
            return gestor;
        });
    }

    @Override
    public List<Gestor> listar() {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_SELECT + " ORDER BY g.orgao, u.nome");
             ResultSet resultado = comando.executeQuery()) {
            List<Gestor> gestores = new ArrayList<>();
            while (resultado.next()) {
                gestores.add((Gestor) UsuarioDao.mapear(resultado));
            }
            return gestores;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar os gestores");
        }
    }

    @Override
    public Optional<Gestor> buscarPorId(long id) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_SELECT + " WHERE g.id_usuario = ?")) {
            comando.setLong(1, id);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next() ? Optional.of((Gestor) UsuarioDao.mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "buscar o gestor");
        }
    }

    @Override
    public boolean atualizar(Gestor gestor) {
        return JdbcUtil.emTransacao("atualizar o gestor", conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(SQL_ATUALIZAR_USUARIO)) {
                comando.setString(1, gestor.getNome());
                comando.setString(2, gestor.getEmail());
                comando.setString(3, gestor.getSenhaHash());
                comando.setLong(4, gestor.getId());
                if (comando.executeUpdate() == 0) {
                    return false;
                }
            }
            try (PreparedStatement comando = conexao.prepareStatement(SQL_ATUALIZAR_GESTOR)) {
                comando.setString(1, gestor.getOrgao().name());
                comando.setString(2, gestor.getMatricula());
                comando.setLong(3, gestor.getId());
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
            throw TradutorErroSql.traduzir(e, "excluir o gestor");
        }
    }

    /** Verifica se a matrícula pertence a outro gestor (ignora o próprio id na edição). */
    public boolean matriculaEmUso(String matricula, Long idIgnorado) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_MATRICULA_EM_USO)) {
            comando.setString(1, matricula);
            comando.setLong(2, idIgnorado == null ? -1 : idIgnorado);
            try (ResultSet resultado = comando.executeQuery()) {
                resultado.next();
                return resultado.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "verificar a matrícula");
        }
    }
}
