package br.com.citizenlife.dao;

import br.com.citizenlife.model.Endereco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD da tabela CL_ENDERECO (casa, trabalho e outros lugares do cidadão). */
public class EnderecoDao implements CrudDao<Endereco> {

    private static final String SELECT = "SELECT id_endereco, id_cidadao, apelido, tipo, descricao, pos_x, pos_y FROM cl_endereco";
    // Casa e trabalho primeiro, depois pelo apelido
    private static final String ORDEM =
            " ORDER BY CASE tipo WHEN 'CASA' THEN 1 WHEN 'TRABALHO' THEN 2 WHEN 'ESTUDO' THEN 3 ELSE 4 END, apelido";

    @Override
    public Endereco inserir(Endereco endereco) {
        String sql = "INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conexao = Conexao.abrir();
             PreparedStatement comando = conexao.prepareStatement(sql, new String[]{"ID_ENDERECO"})) {
            comando.setLong(1, endereco.getIdCidadao());
            preencher(comando, endereco, 2);
            comando.executeUpdate();
            endereco.setId(Conexao.idGerado(comando));
            return endereco;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "salvar o endereço");
        }
    }

    @Override
    public List<Endereco> listar() {
        return consultar(SELECT + ORDEM, null);
    }

    public List<Endereco> listarPorCidadao(long idCidadao) {
        return consultar(SELECT + " WHERE id_cidadao = ?" + ORDEM, idCidadao);
    }

    @Override
    public Optional<Endereco> buscarPorId(long id) {
        return consultar(SELECT + " WHERE id_endereco = ?", id).stream().findFirst();
    }

    @Override
    public boolean atualizar(Endereco endereco) {
        String sql = "UPDATE cl_endereco SET apelido = ?, tipo = ?, descricao = ?, pos_x = ?, pos_y = ? WHERE id_endereco = ?";
        try (Connection conexao = Conexao.abrir(); PreparedStatement comando = conexao.prepareStatement(sql)) {
            preencher(comando, endereco, 1);
            comando.setLong(6, endereco.getId());
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "atualizar o endereço");
        }
    }

    @Override
    public boolean excluir(long id) {
        try (Connection conexao = Conexao.abrir();
             PreparedStatement comando = conexao.prepareStatement("DELETE FROM cl_endereco WHERE id_endereco = ?")) {
            comando.setLong(1, id);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "excluir o endereço");
        }
    }

    /** Apelido, tipo, descrição, x e y ocupam 5 parâmetros seguidos, a partir de "inicio". */
    private void preencher(PreparedStatement comando, Endereco endereco, int inicio) throws SQLException {
        comando.setString(inicio, endereco.getApelido());
        comando.setString(inicio + 1, endereco.getTipo());
        comando.setString(inicio + 2, endereco.getDescricao());
        comando.setDouble(inicio + 3, endereco.getX());
        comando.setDouble(inicio + 4, endereco.getY());
    }

    private List<Endereco> consultar(String sql, Object parametro) {
        try (Connection conexao = Conexao.abrir(); PreparedStatement comando = conexao.prepareStatement(sql)) {
            if (parametro != null) {
                comando.setObject(1, parametro);
            }
            List<Endereco> enderecos = new ArrayList<>();
            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    Endereco endereco = new Endereco();
                    endereco.setId(resultado.getLong("id_endereco"));
                    endereco.setIdCidadao(resultado.getLong("id_cidadao"));
                    endereco.setApelido(resultado.getString("apelido"));
                    endereco.setTipo(resultado.getString("tipo"));
                    endereco.setDescricao(resultado.getString("descricao"));
                    endereco.setX(resultado.getDouble("pos_x"));
                    endereco.setY(resultado.getDouble("pos_y"));
                    enderecos.add(endereco);
                }
            }
            return enderecos;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "consultar os endereços");
        }
    }
}
