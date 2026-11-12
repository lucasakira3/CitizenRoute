package br.com.citizenlife.dao;

import br.com.citizenlife.config.OracleConnectionFactory;
import br.com.citizenlife.model.Cidadao;
import br.com.citizenlife.model.Modal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acesso à tabela CL_CIDADAO: o CRUD do cadastro e as consultas do login. */
public class CidadaoDao implements CrudDao<Cidadao> {

    private static final String SQL_SELECT =
            "SELECT id_cidadao, nome, email, senha_hash, modal, dt_cadastro FROM cl_cidadao";
    private static final String SQL_INSERIR =
            "INSERT INTO cl_cidadao (nome, email, senha_hash, modal, dt_cadastro) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_ATUALIZAR =
            "UPDATE cl_cidadao SET nome = ?, email = ?, senha_hash = ?, modal = ? WHERE id_cidadao = ?";
    // ON DELETE CASCADE remove junto as rotas salvas (e as vias de cada rota) e os endereços.
    private static final String SQL_EXCLUIR = "DELETE FROM cl_cidadao WHERE id_cidadao = ?";
    private static final String SQL_EMAIL_EM_USO =
            "SELECT COUNT(*) FROM cl_cidadao WHERE email = ? AND id_cidadao <> ?";

    @Override
    public Cidadao inserir(Cidadao cidadao) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR, new String[]{"ID_CIDADAO"})) {
            comando.setString(1, cidadao.getNome());
            comando.setString(2, cidadao.getEmail());
            comando.setString(3, cidadao.getSenhaHash());
            comando.setString(4, cidadao.getModal() == null ? null : cidadao.getModal().name());
            comando.setTimestamp(5, JdbcUtil.paraTimestamp(cidadao.getDataCadastro()));
            comando.executeUpdate();
            cidadao.setId(JdbcUtil.idGerado(comando));
            return cidadao;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "cadastrar o cidadão");
        }
    }

    @Override
    public List<Cidadao> listar() {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_SELECT + " ORDER BY nome");
             ResultSet resultado = comando.executeQuery()) {
            List<Cidadao> cidadaos = new ArrayList<>();
            while (resultado.next()) {
                cidadaos.add(mapear(resultado));
            }
            return cidadaos;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar os cidadãos");
        }
    }

    @Override
    public Optional<Cidadao> buscarPorId(long id) {
        return buscarUm(SQL_SELECT + " WHERE id_cidadao = ?", id, "buscar o cidadão");
    }

    public Optional<Cidadao> buscarPorEmail(String email) {
        return buscarUm(SQL_SELECT + " WHERE email = ?", email, "buscar o cidadão pelo e-mail");
    }

    /** Verifica se o e-mail pertence a outro cidadão (ignora o próprio id na edição). */
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

    @Override
    public boolean atualizar(Cidadao cidadao) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_ATUALIZAR)) {
            comando.setString(1, cidadao.getNome());
            comando.setString(2, cidadao.getEmail());
            comando.setString(3, cidadao.getSenhaHash());
            comando.setString(4, cidadao.getModal() == null ? null : cidadao.getModal().name());
            comando.setLong(5, cidadao.getId());
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "atualizar o cidadão");
        }
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

    private Optional<Cidadao> buscarUm(String sql, Object parametro, String contexto) {
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

    private Cidadao mapear(ResultSet resultado) throws SQLException {
        String modal = resultado.getString("modal");
        Cidadao cidadao = new Cidadao(resultado.getLong("id_cidadao"), resultado.getString("nome"),
                resultado.getString("email"), modal == null ? null : Modal.valueOf(modal));
        cidadao.setSenhaHash(resultado.getString("senha_hash"));
        cidadao.setDataCadastro(JdbcUtil.lerData(resultado, "dt_cadastro"));
        return cidadao;
    }
}
