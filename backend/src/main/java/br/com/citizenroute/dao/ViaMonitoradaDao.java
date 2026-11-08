package br.com.citizenroute.dao;

import br.com.citizenroute.config.OracleConnectionFactory;
import br.com.citizenroute.model.ViaMonitorada;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acesso à tabela CR_VIA_MONITORADA (associação N:N entre cidadão e via).
 * A chave é composta (id_cidadao, id_via), por isso não implementa CrudDao.
 */
public class ViaMonitoradaDao {

    private static final String SQL_SELECT = """
            SELECT vm.id_cidadao, vm.apelido, vm.dt_inclusao,
                   v.id_via, v.nome, r.id_regiao, r.nome AS nome_regiao
              FROM cr_via_monitorada vm
              JOIN cr_via v ON v.id_via = vm.id_via
              JOIN cr_regiao r ON r.id_regiao = v.id_regiao
            """;
    private static final String SQL_INSERIR =
            "INSERT INTO cr_via_monitorada (id_cidadao, id_via, apelido, dt_inclusao) VALUES (?, ?, ?, ?)";
    private static final String SQL_ATUALIZAR =
            "UPDATE cr_via_monitorada SET apelido = ? WHERE id_cidadao = ? AND id_via = ?";
    private static final String SQL_EXCLUIR =
            "DELETE FROM cr_via_monitorada WHERE id_cidadao = ? AND id_via = ?";

    public ViaMonitorada inserir(ViaMonitorada viaMonitorada) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR)) {
            comando.setLong(1, viaMonitorada.getIdCidadao());
            comando.setLong(2, viaMonitorada.getVia().getId());
            comando.setString(3, viaMonitorada.getApelido());
            comando.setTimestamp(4, JdbcUtil.paraTimestamp(viaMonitorada.getDataInclusao()));
            comando.executeUpdate();
            return viaMonitorada;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "salvar a via monitorada");
        }
    }

    public List<ViaMonitorada> listarPorCidadao(long idCidadao) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(
                     SQL_SELECT + " WHERE vm.id_cidadao = ? ORDER BY vm.apelido, v.nome")) {
            comando.setLong(1, idCidadao);
            try (ResultSet resultado = comando.executeQuery()) {
                List<ViaMonitorada> vias = new ArrayList<>();
                while (resultado.next()) {
                    vias.add(mapear(resultado));
                }
                return vias;
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar as vias monitoradas");
        }
    }

    public Optional<ViaMonitorada> buscar(long idCidadao, long idVia) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(
                     SQL_SELECT + " WHERE vm.id_cidadao = ? AND vm.id_via = ?")) {
            comando.setLong(1, idCidadao);
            comando.setLong(2, idVia);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "buscar a via monitorada");
        }
    }

    public boolean atualizar(ViaMonitorada viaMonitorada) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_ATUALIZAR)) {
            comando.setString(1, viaMonitorada.getApelido());
            comando.setLong(2, viaMonitorada.getIdCidadao());
            comando.setLong(3, viaMonitorada.getVia().getId());
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "atualizar a via monitorada");
        }
    }

    public boolean excluir(long idCidadao, long idVia) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_EXCLUIR)) {
            comando.setLong(1, idCidadao);
            comando.setLong(2, idVia);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "remover a via monitorada");
        }
    }

    private ViaMonitorada mapear(ResultSet resultado) throws SQLException {
        ViaMonitorada viaMonitorada = new ViaMonitorada(resultado.getLong("id_cidadao"), ViaDao.mapear(resultado),
                resultado.getString("apelido"));
        viaMonitorada.setDataInclusao(JdbcUtil.lerData(resultado, "dt_inclusao"));
        return viaMonitorada;
    }
}
