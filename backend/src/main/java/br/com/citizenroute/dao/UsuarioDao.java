package br.com.citizenroute.dao;

import br.com.citizenroute.config.OracleConnectionFactory;
import br.com.citizenroute.model.Cidadao;
import br.com.citizenroute.model.Gestor;
import br.com.citizenroute.model.Modal;
import br.com.citizenroute.model.Orgao;
import br.com.citizenroute.model.TipoUsuario;
import br.com.citizenroute.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Consultas que valem para qualquer usuário, seja cidadão ou gestor.
 * Faz LEFT JOIN com as duas tabelas filhas e devolve a subclasse certa (polimorfismo).
 */
public class UsuarioDao {

    private static final String SQL_SELECT = """
            SELECT u.id_usuario, u.nome, u.email, u.senha_hash, u.tipo, u.dt_cadastro,
                   c.modal, g.orgao, g.matricula
              FROM cr_usuario u
              LEFT JOIN cr_cidadao c ON c.id_usuario = u.id_usuario
              LEFT JOIN cr_gestor g ON g.id_usuario = u.id_usuario
            """;
    private static final String SQL_EMAIL_EM_USO =
            "SELECT COUNT(*) FROM cr_usuario WHERE email = ? AND id_usuario <> ?";

    public Optional<Usuario> buscarPorId(long id) {
        return buscarUm(SQL_SELECT + " WHERE u.id_usuario = ?", id, "buscar o usuário");
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return buscarUm(SQL_SELECT + " WHERE u.email = ?", email, "buscar o usuário pelo e-mail");
    }

    /** Verifica se o e-mail pertence a outro usuário (ignora o próprio id na edição). */
    public boolean emailEmUso(String email, Long idIgnorado) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_EMAIL_EM_USO)) {
            comando.setString(1, email);
            comando.setLong(2, idIgnorado == null ? -1 : idIgnorado);
            try (ResultSet resultado = comando.executeQuery()) {
                resultado.next();
                return resultado.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "verificar o e-mail");
        }
    }

    private Optional<Usuario> buscarUm(String sql, Object parametro, String contexto) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setObject(1, parametro);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, contexto);
        }
    }

    /** Cria um Cidadao ou um Gestor conforme a coluna TIPO. */
    static Usuario mapear(ResultSet resultado) throws SQLException {
        TipoUsuario tipo = TipoUsuario.valueOf(resultado.getString("tipo"));
        Usuario usuario;
        if (tipo == TipoUsuario.CIDADAO) {
            Cidadao cidadao = new Cidadao();
            cidadao.setModal(Modal.valueOf(resultado.getString("modal")));
            usuario = cidadao;
        } else {
            Gestor gestor = new Gestor();
            gestor.setOrgao(Orgao.valueOf(resultado.getString("orgao")));
            gestor.setMatricula(resultado.getString("matricula"));
            usuario = gestor;
        }
        usuario.setId(resultado.getLong("id_usuario"));
        usuario.setNome(resultado.getString("nome"));
        usuario.setEmail(resultado.getString("email"));
        usuario.setSenhaHash(resultado.getString("senha_hash"));
        usuario.setDataCadastro(JdbcUtil.lerData(resultado, "dt_cadastro"));
        return usuario;
    }
}
