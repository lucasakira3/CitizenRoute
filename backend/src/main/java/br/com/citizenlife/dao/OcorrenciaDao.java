package br.com.citizenlife.dao;

import br.com.citizenlife.config.OracleConnectionFactory;
import br.com.citizenlife.model.Cidadao;
import br.com.citizenlife.model.Modal;
import br.com.citizenlife.model.NivelImpacto;
import br.com.citizenlife.model.Ocorrencia;
import br.com.citizenlife.model.OcorrenciaVia;
import br.com.citizenlife.model.StatusOcorrencia;
import br.com.citizenlife.model.TipoOcorrencia;
import br.com.citizenlife.model.Via;

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
 * Consulta as tabelas CL_OCORRENCIA e CL_OCORRENCIA_VIA (associação N:N com CL_VIA).
 * As ocorrências alimentam o mapa de riscos e os alertas das rotas.
 */
public class OcorrenciaDao {

    private static final String ATIVAS = "('ABERTA', 'CONFIRMADA', 'EM_ATENDIMENTO')";

    // LEFT JOIN: quando o autor exclui a conta, o relato continua no mapa sem autor (ON DELETE SET NULL).
    private static final String SQL_SELECT = """
            SELECT o.id_ocorrencia, o.titulo, o.descricao, o.tipo, o.gravidade, o.status,
                   o.dt_registro, o.dt_atualizacao, o.id_cidadao_autor,
                   c.nome AS autor_nome, c.modal AS autor_modal
              FROM cl_ocorrencia o
              LEFT JOIN cl_cidadao c ON c.id_cidadao = o.id_cidadao_autor
            """;
    private static final String SQL_SELECT_VIAS = """
            SELECT ov.id_ocorrencia, ov.nivel_impacto, v.id_via, v.nome, r.id_regiao, r.nome AS nome_regiao
              FROM cl_ocorrencia_via ov
              JOIN cl_via v ON v.id_via = ov.id_via
              JOIN cl_regiao r ON r.id_regiao = v.id_regiao
             WHERE ov.id_ocorrencia IN (%s)
             ORDER BY v.nome
            """;

    private static final int LOTE_IN = 500;

    /** Lista ocorrências aplicando os filtros; as ativas aparecem primeiro e as mais recentes no topo. */
    public List<Ocorrencia> listar(FiltroOcorrencia filtro) {
        StringBuilder sql = new StringBuilder(SQL_SELECT).append(" WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();

        if (filtro.apenasAtivas()) {
            sql.append(" AND o.status IN ").append(ATIVAS);
        }
        if (filtro.idCidadaoComRota() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM cl_ocorrencia_via ov"
                    + " JOIN cl_rota_via rv ON rv.id_via = ov.id_via JOIN cl_rota ro ON ro.id_rota = rv.id_rota"
                    + " WHERE ov.id_ocorrencia = o.id_ocorrencia AND ro.id_cidadao = ?)");
            parametros.add(filtro.idCidadaoComRota());
        }
        sql.append(" ORDER BY CASE WHEN o.status IN ").append(ATIVAS)
                .append(" THEN 0 ELSE 1 END, o.dt_registro DESC, o.id_ocorrencia DESC");

        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                comando.setObject(i + 1, parametros.get(i));
            }
            List<Ocorrencia> ocorrencias = new ArrayList<>();
            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    ocorrencias.add(mapear(resultado));
                }
            }
            carregarVias(conexao, ocorrencias);
            return ocorrencias;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar as ocorrências");
        }
    }

    public Optional<Ocorrencia> buscarPorId(long id) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_SELECT + " WHERE o.id_ocorrencia = ?")) {
            comando.setLong(1, id);
            Ocorrencia ocorrencia;
            try (ResultSet resultado = comando.executeQuery()) {
                if (!resultado.next()) {
                    return Optional.empty();
                }
                ocorrencia = mapear(resultado);
            }
            carregarVias(conexao, List.of(ocorrencia));
            return Optional.of(ocorrencia);
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "buscar a ocorrência");
        }
    }

    /** Busca as vias de várias ocorrências com uma consulta por lote, em vez de uma por ocorrência. */
    private void carregarVias(Connection conexao, List<Ocorrencia> ocorrencias) throws SQLException {
        Map<Long, Ocorrencia> porId = new LinkedHashMap<>();
        ocorrencias.forEach(ocorrencia -> porId.put(ocorrencia.getId(), ocorrencia));
        List<Long> ids = new ArrayList<>(porId.keySet());

        for (int inicio = 0; inicio < ids.size(); inicio += LOTE_IN) {
            List<Long> lote = ids.subList(inicio, Math.min(inicio + LOTE_IN, ids.size()));
            String sql = SQL_SELECT_VIAS.formatted(JdbcUtil.marcadores(lote.size()));
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                for (int i = 0; i < lote.size(); i++) {
                    comando.setLong(i + 1, lote.get(i));
                }
                try (ResultSet resultado = comando.executeQuery()) {
                    while (resultado.next()) {
                        Via via = ViaDao.mapear(resultado);
                        NivelImpacto nivel = NivelImpacto.valueOf(resultado.getString("nivel_impacto"));
                        porId.get(resultado.getLong("id_ocorrencia")).getVias().add(new OcorrenciaVia(via, nivel));
                    }
                }
            }
        }
    }

    private Ocorrencia mapear(ResultSet resultado) throws SQLException {
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setId(resultado.getLong("id_ocorrencia"));
        ocorrencia.setTitulo(resultado.getString("titulo"));
        ocorrencia.setDescricao(resultado.getString("descricao"));
        ocorrencia.setTipo(TipoOcorrencia.valueOf(resultado.getString("tipo")));
        ocorrencia.setGravidade(resultado.getInt("gravidade"));
        ocorrencia.setStatus(StatusOcorrencia.valueOf(resultado.getString("status")));
        ocorrencia.setDataRegistro(JdbcUtil.lerData(resultado, "dt_registro"));
        ocorrencia.setDataAtualizacao(JdbcUtil.lerData(resultado, "dt_atualizacao"));
        long idAutor = resultado.getLong("id_cidadao_autor");
        if (!resultado.wasNull()) {
            String modal = resultado.getString("autor_modal");
            ocorrencia.setAutor(new Cidadao(idAutor, resultado.getString("autor_nome"), null,
                    modal == null ? null : Modal.valueOf(modal)));
        }
        return ocorrencia;
    }
}
