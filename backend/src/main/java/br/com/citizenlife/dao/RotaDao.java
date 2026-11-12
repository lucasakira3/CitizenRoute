package br.com.citizenlife.dao;

import br.com.citizenlife.model.Rota;
import br.com.citizenlife.model.Via;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD das tabelas CL_ROTA e CL_ROTA_VIA (N:N entre rota e via, com a ordem no caminho).
 * A rota e as suas vias são gravadas juntas, na mesma transação. Também lista as vias de CL_VIA.
 */
public class RotaDao implements CrudDao<Rota> {

    private static final String SELECT = """
            SELECT id_rota, id_cidadao, nome, origem_descricao, origem_x, origem_y,
                   destino_descricao, destino_x, destino_y, distancia_m, dt_criacao
              FROM cl_rota""";

    @Override
    public Rota inserir(Rota rota) {
        String sql = """
                INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y,
                                     destino_descricao, destino_x, destino_y, distancia_m, dt_criacao)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        try (Connection conexao = Conexao.abrir()) {
            conexao.setAutoCommit(false);
            try (PreparedStatement comando = conexao.prepareStatement(sql, new String[]{"ID_ROTA"})) {
                comando.setLong(1, rota.getIdCidadao());
                comando.setString(2, rota.getNome());
                preencherPontos(comando, rota, 3);
                comando.setTimestamp(10, Timestamp.valueOf(rota.getDataCriacao()));
                comando.executeUpdate();
                rota.setId(Conexao.idGerado(comando));
                gravarVias(conexao, rota);
                conexao.commit();
                return rota;
            } catch (SQLException e) {
                conexao.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "salvar a rota");
        }
    }

    @Override
    public List<Rota> listar() {
        return consultar(SELECT + " ORDER BY nome", null);
    }

    public List<Rota> listarPorCidadao(long idCidadao) {
        return consultar(SELECT + " WHERE id_cidadao = ? ORDER BY nome", idCidadao);
    }

    @Override
    public Optional<Rota> buscarPorId(long id) {
        return consultar(SELECT + " WHERE id_rota = ?", id).stream().findFirst();
    }

    /** Atualiza a rota e troca a lista de vias, na mesma transação. */
    @Override
    public boolean atualizar(Rota rota) {
        String sql = """
                UPDATE cl_rota SET nome = ?, origem_descricao = ?, origem_x = ?, origem_y = ?,
                       destino_descricao = ?, destino_x = ?, destino_y = ?, distancia_m = ?
                 WHERE id_rota = ?""";
        try (Connection conexao = Conexao.abrir()) {
            conexao.setAutoCommit(false);
            try (PreparedStatement comando = conexao.prepareStatement(sql);
                 PreparedStatement apagarVias = conexao.prepareStatement("DELETE FROM cl_rota_via WHERE id_rota = ?")) {
                comando.setString(1, rota.getNome());
                preencherPontos(comando, rota, 2);
                comando.setLong(9, rota.getId());
                if (comando.executeUpdate() == 0) {
                    conexao.rollback();
                    return false;
                }
                apagarVias.setLong(1, rota.getId());
                apagarVias.executeUpdate();
                gravarVias(conexao, rota);
                conexao.commit();
                return true;
            } catch (SQLException e) {
                conexao.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "atualizar a rota");
        }
    }

    /** ON DELETE CASCADE apaga junto as linhas de CL_ROTA_VIA. */
    @Override
    public boolean excluir(long id) {
        try (Connection conexao = Conexao.abrir();
             PreparedStatement comando = conexao.prepareStatement("DELETE FROM cl_rota WHERE id_rota = ?")) {
            comando.setLong(1, id);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "excluir a rota");
        }
    }

    /** Todas as vias cadastradas (CL_VIA), em ordem alfabética. */
    public List<Via> listarVias() {
        try (Connection conexao = Conexao.abrir();
             PreparedStatement comando = conexao.prepareStatement("SELECT id_via, nome, zona FROM cl_via ORDER BY nome");
             ResultSet resultado = comando.executeQuery()) {
            List<Via> vias = new ArrayList<>();
            while (resultado.next()) {
                vias.add(new Via(resultado.getLong("id_via"), resultado.getString("nome"), resultado.getString("zona")));
            }
            return vias;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "listar as vias");
        }
    }

    /** Saída, destino e distância ocupam 7 parâmetros seguidos, a partir de "inicio". */
    private void preencherPontos(PreparedStatement comando, Rota rota, int inicio) throws SQLException {
        comando.setString(inicio, rota.getOrigemDescricao());
        comando.setDouble(inicio + 1, rota.getOrigemX());
        comando.setDouble(inicio + 2, rota.getOrigemY());
        comando.setString(inicio + 3, rota.getDestinoDescricao());
        comando.setDouble(inicio + 4, rota.getDestinoX());
        comando.setDouble(inicio + 5, rota.getDestinoY());
        comando.setInt(inicio + 6, rota.getDistanciaMetros());
    }

    private void gravarVias(Connection conexao, Rota rota) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(
                "INSERT INTO cl_rota_via (id_rota, id_via, ordem) VALUES (?, ?, ?)")) {
            int ordem = 1;
            for (Via via : rota.getVias()) {
                comando.setLong(1, rota.getId());
                comando.setLong(2, via.getId());
                comando.setInt(3, ordem++);
                comando.addBatch();
            }
            comando.executeBatch();
        }
    }

    private List<Rota> consultar(String sql, Object parametro) {
        try (Connection conexao = Conexao.abrir(); PreparedStatement comando = conexao.prepareStatement(sql)) {
            if (parametro != null) {
                comando.setObject(1, parametro);
            }
            List<Rota> rotas = new ArrayList<>();
            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    rotas.add(mapear(resultado));
                }
            }
            // As vias de cada rota, na ordem do caminho (JOIN da associativa com CL_VIA)
            try (PreparedStatement vias = conexao.prepareStatement("""
                    SELECT v.id_via, v.nome, v.zona FROM cl_rota_via rv JOIN cl_via v ON v.id_via = rv.id_via
                     WHERE rv.id_rota = ? ORDER BY rv.ordem""")) {
                for (Rota rota : rotas) {
                    vias.setLong(1, rota.getId());
                    try (ResultSet resultado = vias.executeQuery()) {
                        while (resultado.next()) {
                            rota.getVias().add(new Via(resultado.getLong("id_via"), resultado.getString("nome"),
                                    resultado.getString("zona")));
                        }
                    }
                }
            }
            return rotas;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "consultar as rotas");
        }
    }

    private Rota mapear(ResultSet resultado) throws SQLException {
        Rota rota = new Rota();
        rota.setId(resultado.getLong("id_rota"));
        rota.setIdCidadao(resultado.getLong("id_cidadao"));
        rota.setNome(resultado.getString("nome"));
        rota.setOrigemDescricao(resultado.getString("origem_descricao"));
        rota.setOrigemX(resultado.getDouble("origem_x"));
        rota.setOrigemY(resultado.getDouble("origem_y"));
        rota.setDestinoDescricao(resultado.getString("destino_descricao"));
        rota.setDestinoX(resultado.getDouble("destino_x"));
        rota.setDestinoY(resultado.getDouble("destino_y"));
        rota.setDistanciaMetros(resultado.getInt("distancia_m"));
        rota.setDataCriacao(Conexao.data(resultado, "dt_criacao"));
        return rota;
    }
}
