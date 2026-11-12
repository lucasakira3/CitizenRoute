package br.com.citizenlife.dao;

import br.com.citizenlife.model.Cidadao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD da tabela CL_CIDADAO. */
public class CidadaoDao implements CrudDao<Cidadao> {

    private static final String SELECT = "SELECT id_cidadao, nome, email, senha_hash, modal, dt_cadastro FROM cl_cidadao";

    @Override
    public Cidadao inserir(Cidadao cidadao) {
        String sql = "INSERT INTO cl_cidadao (nome, email, senha_hash, modal, dt_cadastro) VALUES (?, ?, ?, ?, ?)";
        try (Connection conexao = Conexao.abrir();
             PreparedStatement comando = conexao.prepareStatement(sql, new String[]{"ID_CIDADAO"})) {
            comando.setString(1, cidadao.getNome());
            comando.setString(2, cidadao.getEmail());
            comando.setString(3, cidadao.getSenhaHash());
            comando.setString(4, cidadao.getModal());
            comando.setTimestamp(5, Timestamp.valueOf(cidadao.getDataCadastro()));
            comando.executeUpdate();
            cidadao.setId(Conexao.idGerado(comando));
            return cidadao;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "cadastrar o cidadão");
        }
    }

    @Override
    public List<Cidadao> listar() {
        return consultar(SELECT + " ORDER BY nome", null);
    }

    @Override
    public Optional<Cidadao> buscarPorId(long id) {
        return consultar(SELECT + " WHERE id_cidadao = ?", id).stream().findFirst();
    }

    public Optional<Cidadao> buscarPorEmail(String email) {
        return consultar(SELECT + " WHERE email = ?", email).stream().findFirst();
    }

    @Override
    public boolean atualizar(Cidadao cidadao) {
        String sql = "UPDATE cl_cidadao SET nome = ?, email = ?, senha_hash = ?, modal = ? WHERE id_cidadao = ?";
        try (Connection conexao = Conexao.abrir(); PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, cidadao.getNome());
            comando.setString(2, cidadao.getEmail());
            comando.setString(3, cidadao.getSenhaHash());
            comando.setString(4, cidadao.getModal());
            comando.setLong(5, cidadao.getId());
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "atualizar o cidadão");
        }
    }

    /** ON DELETE CASCADE apaga junto as rotas e os endereços do cidadão. */
    @Override
    public boolean excluir(long id) {
        try (Connection conexao = Conexao.abrir();
             PreparedStatement comando = conexao.prepareStatement("DELETE FROM cl_cidadao WHERE id_cidadao = ?")) {
            comando.setLong(1, id);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "excluir o cidadão");
        }
    }

    private List<Cidadao> consultar(String sql, Object parametro) {
        try (Connection conexao = Conexao.abrir(); PreparedStatement comando = conexao.prepareStatement(sql)) {
            if (parametro != null) {
                comando.setObject(1, parametro);
            }
            List<Cidadao> cidadaos = new ArrayList<>();
            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    Cidadao cidadao = new Cidadao(resultado.getLong("id_cidadao"), resultado.getString("nome"),
                            resultado.getString("email"), resultado.getString("modal"));
                    cidadao.setSenhaHash(resultado.getString("senha_hash"));
                    cidadao.setDataCadastro(Conexao.data(resultado, "dt_cadastro"));
                    cidadaos.add(cidadao);
                }
            }
            return cidadaos;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "consultar os cidadãos");
        }
    }
}
