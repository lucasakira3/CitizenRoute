package br.com.citizenroute.dao;

import br.com.citizenroute.config.OracleConnectionFactory;
import br.com.citizenroute.model.CriterioRota;
import br.com.citizenroute.model.PontoRota;
import br.com.citizenroute.model.Rota;
import br.com.citizenroute.model.Via;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Acesso às tabelas CR_ROTA e CR_ROTA_VIA (associação N:N entre rota e via, com a ordem no caminho).
 * A rota e as suas vias são sempre gravadas juntas, na mesma transação.
 */
public class RotaDao implements CrudDao<Rota> {

    private static final String SQL_SELECT = """
            SELECT id_rota, id_cidadao, nome, origem_descricao, origem_x, origem_y,
                   destino_descricao, destino_x, destino_y, criterio, distancia_m, dt_criacao
              FROM cr_rota
            """;
    private static final String SQL_SELECT_VIAS = """
            SELECT rv.id_rota, v.id_via, v.nome, r.id_regiao, r.nome AS nome_regiao
              FROM cr_rota_via rv
              JOIN cr_via v ON v.id_via = rv.id_via
              JOIN cr_regiao r ON r.id_regiao = v.id_regiao
             WHERE rv.id_rota IN (%s)
             ORDER BY rv.id_rota, rv.ordem
            """;
    private static final String SQL_INSERIR = """
            INSERT INTO cr_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y,
                                 destino_descricao, destino_x, destino_y, criterio, distancia_m, dt_criacao)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String SQL_INSERIR_VIA =
            "INSERT INTO cr_rota_via (id_rota, id_via, ordem) VALUES (?, ?, ?)";
    private static final String SQL_ATUALIZAR = """
            UPDATE cr_rota
               SET nome = ?, origem_descricao = ?, origem_x = ?, origem_y = ?,
                   destino_descricao = ?, destino_x = ?, destino_y = ?, criterio = ?, distancia_m = ?
             WHERE id_rota = ?
            """;
    private static final String SQL_EXCLUIR_VIAS = "DELETE FROM cr_rota_via WHERE id_rota = ?";
    // ON DELETE CASCADE remove junto as linhas de CR_ROTA_VIA.
    private static final String SQL_EXCLUIR = "DELETE FROM cr_rota WHERE id_rota = ?";

    @Override
    public Rota inserir(Rota rota) {
        return JdbcUtil.emTransacao("salvar a rota", conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR, new String[]{"ID_ROTA"})) {
                comando.setLong(1, rota.getIdCidadao());
                comando.setString(2, rota.getNome());
                preencherPontos(comando, rota, 3);
                comando.setTimestamp(11, JdbcUtil.paraTimestamp(rota.getDataCriacao()));
                comando.executeUpdate();
                rota.setId(JdbcUtil.idGerado(comando));
            }
            inserirVias(conexao, rota);
            return rota;
        });
    }

    @Override
    public List<Rota> listar() {
        return consultar(SQL_SELECT + " ORDER BY id_cidadao, nome", null);
    }

    /** Rotas de um cidadão, em ordem alfabética. */
    public List<Rota> listarPorCidadao(long idCidadao) {
        return consultar(SQL_SELECT + " WHERE id_cidadao = ? ORDER BY nome", idCidadao);
    }

    @Override
    public Optional<Rota> buscarPorId(long id) {
        return consultar(SQL_SELECT + " WHERE id_rota = ?", id).stream().findFirst();
    }

    /** Atualiza os dados da rota e substitui a lista de vias, tudo em uma transação. */
    @Override
    public boolean atualizar(Rota rota) {
        return JdbcUtil.emTransacao("atualizar a rota", conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(SQL_ATUALIZAR)) {
                comando.setString(1, rota.getNome());
                preencherPontos(comando, rota, 2);
                comando.setLong(10, rota.getId());
                if (comando.executeUpdate() == 0) {
                    return false;
                }
            }
            try (PreparedStatement comando = conexao.prepareStatement(SQL_EXCLUIR_VIAS)) {
                comando.setLong(1, rota.getId());
                comando.executeUpdate();
            }
            inserirVias(conexao, rota);
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
            throw TradutorErroSql.traduzir(e, "excluir a rota");
        }
    }

    /** Saída, destino, critério e distância ocupam 8 parâmetros seguidos, a partir de "inicio". */
    private void preencherPontos(PreparedStatement comando, Rota rota, int inicio) throws SQLException {
        comando.setString(inicio, rota.getOrigem().descricao());
        comando.setDouble(inicio + 1, rota.getOrigem().x());
        comando.setDouble(inicio + 2, rota.getOrigem().y());
        comando.setString(inicio + 3, rota.getDestino().descricao());
        comando.setDouble(inicio + 4, rota.getDestino().x());
        comando.setDouble(inicio + 5, rota.getDestino().y());
        comando.setString(inicio + 6, rota.getCriterio().name());
        comando.setInt(inicio + 7, rota.getDistanciaMetros());
    }

    private void inserirVias(Connection conexao, Rota rota) throws SQLException {
        if (rota.getVias().isEmpty()) {
            return;
        }
        try (PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR_VIA)) {
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

    private List<Rota> consultar(String sql, Long parametro) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            if (parametro != null) {
                comando.setLong(1, parametro);
            }
            List<Rota> rotas = new ArrayList<>();
            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    rotas.add(mapear(resultado));
                }
            }
            carregarVias(conexao, rotas);
            return rotas;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "consultar as rotas");
        }
    }

    /** Busca as vias de várias rotas com uma única consulta, em vez de uma por rota. */
    private void carregarVias(Connection conexao, List<Rota> rotas) throws SQLException {
        if (rotas.isEmpty()) {
            return;
        }
        Map<Long, Rota> porId = new LinkedHashMap<>();
        rotas.forEach(rota -> porId.put(rota.getId(), rota));
        String sql = SQL_SELECT_VIAS.formatted(JdbcUtil.marcadores(porId.size()));
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            int indice = 1;
            for (Long id : porId.keySet()) {
                comando.setLong(indice++, id);
            }
            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    porId.get(resultado.getLong("id_rota")).getVias().add(ViaDao.mapear(resultado));
                }
            }
        }
    }

    private Rota mapear(ResultSet resultado) throws SQLException {
        Rota rota = new Rota(
                resultado.getLong("id_rota"),
                resultado.getLong("id_cidadao"),
                resultado.getString("nome"),
                new PontoRota(resultado.getString("origem_descricao"),
                        resultado.getDouble("origem_x"), resultado.getDouble("origem_y")),
                new PontoRota(resultado.getString("destino_descricao"),
                        resultado.getDouble("destino_x"), resultado.getDouble("destino_y")),
                CriterioRota.valueOf(resultado.getString("criterio")),
                resultado.getInt("distancia_m"));
        rota.setDataCriacao(JdbcUtil.lerData(resultado, "dt_criacao"));
        return rota;
    }
}
