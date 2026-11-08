package br.com.citizenroute.dao;

import br.com.citizenroute.config.OracleConnectionFactory;
import br.com.citizenroute.model.Acao;
import br.com.citizenroute.model.Cidadao;
import br.com.citizenroute.model.Modal;
import br.com.citizenroute.model.NivelImpacto;
import br.com.citizenroute.model.Ocorrencia;
import br.com.citizenroute.model.OcorrenciaVia;
import br.com.citizenroute.model.StatusOcorrencia;
import br.com.citizenroute.model.TipoOcorrencia;
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
 * Acesso às tabelas CR_OCORRENCIA e CR_OCORRENCIA_VIA (associação N:N com CR_VIA).
 * A ocorrência e as vias que ela atinge são sempre gravadas juntas, na mesma transação.
 */
public class OcorrenciaDao implements CrudDao<Ocorrencia> {

    private static final String ATIVAS = "('ABERTA', 'CONFIRMADA', 'EM_ATENDIMENTO')";

    private static final String SQL_SELECT = """
            SELECT o.id_ocorrencia, o.titulo, o.descricao, o.tipo, o.gravidade, o.status,
                   o.dt_registro, o.dt_atualizacao, o.id_cidadao_autor,
                   u.nome AS autor_nome, c.modal AS autor_modal
              FROM cr_ocorrencia o
              JOIN cr_cidadao c ON c.id_usuario = o.id_cidadao_autor
              JOIN cr_usuario u ON u.id_usuario = c.id_usuario
            """;
    private static final String SQL_SELECT_VIAS = """
            SELECT ov.id_ocorrencia, ov.nivel_impacto, v.id_via, v.nome, r.id_regiao, r.nome AS nome_regiao
              FROM cr_ocorrencia_via ov
              JOIN cr_via v ON v.id_via = ov.id_via
              JOIN cr_regiao r ON r.id_regiao = v.id_regiao
             WHERE ov.id_ocorrencia IN (%s)
             ORDER BY v.nome
            """;
    private static final String SQL_INSERIR = """
            INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String SQL_INSERIR_VIA =
            "INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto) VALUES (?, ?, ?)";
    private static final String SQL_EXCLUIR_VIAS = "DELETE FROM cr_ocorrencia_via WHERE id_ocorrencia = ?";
    private static final String SQL_ATUALIZAR = """
            UPDATE cr_ocorrencia
               SET titulo = ?, descricao = ?, tipo = ?, gravidade = ?, dt_atualizacao = ?
             WHERE id_ocorrencia = ?
            """;
    // A condição "AND status = ?" evita sobrescrever uma mudança feita por outro gestor ao mesmo tempo.
    private static final String SQL_ATUALIZAR_STATUS = """
            UPDATE cr_ocorrencia
               SET status = ?, dt_atualizacao = ?
             WHERE id_ocorrencia = ? AND status = ?
            """;
    // ON DELETE CASCADE remove junto as linhas de CR_OCORRENCIA_VIA e CR_ACAO.
    private static final String SQL_EXCLUIR = "DELETE FROM cr_ocorrencia WHERE id_ocorrencia = ?";

    private static final int LOTE_IN = 500;

    private final AcaoDao acaoDao;

    public OcorrenciaDao(AcaoDao acaoDao) {
        this.acaoDao = acaoDao;
    }

    @Override
    public Ocorrencia inserir(Ocorrencia ocorrencia) {
        return JdbcUtil.emTransacao("registrar a ocorrência", conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR, new String[]{"ID_OCORRENCIA"})) {
                comando.setString(1, ocorrencia.getTitulo());
                comando.setString(2, ocorrencia.getDescricao());
                comando.setString(3, ocorrencia.getTipo().name());
                comando.setInt(4, ocorrencia.getGravidade());
                comando.setString(5, ocorrencia.getStatus().name());
                comando.setTimestamp(6, JdbcUtil.paraTimestamp(ocorrencia.getDataRegistro()));
                comando.setLong(7, ocorrencia.getAutor().getId());
                comando.executeUpdate();
                ocorrencia.setId(JdbcUtil.idGerado(comando));
            }
            inserirVias(conexao, ocorrencia);
            return ocorrencia;
        });
    }

    @Override
    public List<Ocorrencia> listar() {
        return listar(FiltroOcorrencia.todas());
    }

    /** Lista ocorrências aplicando os filtros; as ativas aparecem primeiro e as mais recentes no topo. */
    public List<Ocorrencia> listar(FiltroOcorrencia filtro) {
        StringBuilder sql = new StringBuilder(SQL_SELECT).append(" WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();

        if (filtro.apenasAtivas()) {
            sql.append(" AND o.status IN ").append(ATIVAS);
        }
        if (filtro.status() != null) {
            sql.append(" AND o.status = ?");
            parametros.add(filtro.status().name());
        }
        if (filtro.tipos() != null && !filtro.tipos().isEmpty()) {
            sql.append(" AND o.tipo IN (").append(JdbcUtil.marcadores(filtro.tipos().size())).append(")");
            filtro.tipos().forEach(tipo -> parametros.add(tipo.name()));
        }
        if (filtro.idAutor() != null) {
            sql.append(" AND o.id_cidadao_autor = ?");
            parametros.add(filtro.idAutor());
        }
        if (filtro.idVia() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM cr_ocorrencia_via ov"
                    + " WHERE ov.id_ocorrencia = o.id_ocorrencia AND ov.id_via = ?)");
            parametros.add(filtro.idVia());
        }
        if (filtro.idRegiao() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM cr_ocorrencia_via ov JOIN cr_via v ON v.id_via = ov.id_via"
                    + " WHERE ov.id_ocorrencia = o.id_ocorrencia AND v.id_regiao = ?)");
            parametros.add(filtro.idRegiao());
        }
        if (filtro.idCidadaoMonitorando() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM cr_ocorrencia_via ov"
                    + " JOIN cr_via_monitorada vm ON vm.id_via = ov.id_via"
                    + " WHERE ov.id_ocorrencia = o.id_ocorrencia AND vm.id_cidadao = ?)");
            parametros.add(filtro.idCidadaoMonitorando());
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

    @Override
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

    /** Atualiza os dados da ocorrência e substitui a lista de vias, tudo em uma transação. */
    @Override
    public boolean atualizar(Ocorrencia ocorrencia) {
        return JdbcUtil.emTransacao("atualizar a ocorrência", conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(SQL_ATUALIZAR)) {
                comando.setString(1, ocorrencia.getTitulo());
                comando.setString(2, ocorrencia.getDescricao());
                comando.setString(3, ocorrencia.getTipo().name());
                comando.setInt(4, ocorrencia.getGravidade());
                comando.setTimestamp(5, JdbcUtil.paraTimestamp(ocorrencia.getDataAtualizacao()));
                comando.setLong(6, ocorrencia.getId());
                if (comando.executeUpdate() == 0) {
                    return false;
                }
            }
            try (PreparedStatement comando = conexao.prepareStatement(SQL_EXCLUIR_VIAS)) {
                comando.setLong(1, ocorrencia.getId());
                comando.executeUpdate();
            }
            inserirVias(conexao, ocorrencia);
            return true;
        });
    }

    /**
     * Muda o status e registra a ação do gestor na mesma transação.
     * Devolve falso, sem gravar nada, se o status no banco já não for o status anterior esperado.
     */
    public boolean alterarStatus(Ocorrencia ocorrencia, StatusOcorrencia statusAnterior, Acao acao) {
        return JdbcUtil.emTransacao("alterar o status da ocorrência", conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(SQL_ATUALIZAR_STATUS)) {
                comando.setString(1, ocorrencia.getStatus().name());
                comando.setTimestamp(2, JdbcUtil.paraTimestamp(ocorrencia.getDataAtualizacao()));
                comando.setLong(3, ocorrencia.getId());
                comando.setString(4, statusAnterior.name());
                if (comando.executeUpdate() == 0) {
                    conexao.rollback();
                    return false;
                }
            }
            acaoDao.inserir(conexao, acao);
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
            throw TradutorErroSql.traduzir(e, "excluir a ocorrência");
        }
    }

    private void inserirVias(Connection conexao, Ocorrencia ocorrencia) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR_VIA)) {
            for (OcorrenciaVia item : ocorrencia.getVias()) {
                comando.setLong(1, ocorrencia.getId());
                comando.setLong(2, item.getVia().getId());
                comando.setString(3, item.getNivelImpacto().name());
                comando.addBatch();
            }
            comando.executeBatch();
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
        ocorrencia.setAutor(new Cidadao(resultado.getLong("id_cidadao_autor"), resultado.getString("autor_nome"),
                null, Modal.valueOf(resultado.getString("autor_modal"))));
        return ocorrencia;
    }
}
