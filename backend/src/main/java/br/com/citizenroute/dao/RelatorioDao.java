package br.com.citizenroute.dao;

import br.com.citizenroute.config.OracleConnectionFactory;
import br.com.citizenroute.model.Modal;
import br.com.citizenroute.model.Orgao;
import br.com.citizenroute.model.TipoOcorrencia;
import br.com.citizenroute.model.relatorio.ExposicaoModal;
import br.com.citizenroute.model.relatorio.OcorrenciasPorRegiao;
import br.com.citizenroute.model.relatorio.ResumoPainel;
import br.com.citizenroute.model.relatorio.TempoRespostaOrgao;
import br.com.citizenroute.model.relatorio.ViaCritica;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Relatórios gerenciais. Cada método é uma consulta com JOIN e agregação (COUNT, SUM, AVG, MAX, MIN).
 * Ocorrências descartadas ficam fora das contagens, pois eram relatos inválidos.
 */
public class RelatorioDao {

    private static final String SQL_VIAS_CRITICAS = """
            SELECT v.id_via, v.nome AS via, r.nome AS regiao,
                   COUNT(o.id_ocorrencia) AS total,
                   SUM(CASE WHEN o.status IN ('ABERTA', 'CONFIRMADA', 'EM_ATENDIMENTO') THEN 1 ELSE 0 END) AS ativas,
                   ROUND(AVG(o.gravidade), 1) AS gravidade_media,
                   MAX(o.gravidade) AS gravidade_maxima
              FROM cr_via v
              JOIN cr_regiao r ON r.id_regiao = v.id_regiao
              JOIN cr_ocorrencia_via ov ON ov.id_via = v.id_via
              JOIN cr_ocorrencia o ON o.id_ocorrencia = ov.id_ocorrencia
             WHERE o.status <> 'DESCARTADA'
             GROUP BY v.id_via, v.nome, r.nome
             ORDER BY ativas DESC, total DESC, gravidade_media DESC
             FETCH FIRST 10 ROWS ONLY
            """;

    // COUNT(DISTINCT ...) evita contar duas vezes uma ocorrência que atinge duas vias da mesma região.
    private static final String SQL_POR_REGIAO = """
            SELECT r.nome AS regiao, o.tipo,
                   COUNT(DISTINCT o.id_ocorrencia) AS total,
                   COUNT(DISTINCT CASE WHEN o.status IN ('ABERTA', 'CONFIRMADA', 'EM_ATENDIMENTO')
                                       THEN o.id_ocorrencia END) AS ativas
              FROM cr_ocorrencia o
              JOIN cr_ocorrencia_via ov ON ov.id_ocorrencia = o.id_ocorrencia
              JOIN cr_via v ON v.id_via = ov.id_via
              JOIN cr_regiao r ON r.id_regiao = v.id_regiao
             WHERE o.status <> 'DESCARTADA'
             GROUP BY r.nome, o.tipo
             ORDER BY r.nome, total DESC
            """;

    // A subconsulta acha a primeira ação de cada órgão em cada ocorrência.
    // No Oracle, DATE - DATE devolve dias; vezes 1440 dá minutos.
    private static final String SQL_TEMPO_RESPOSTA = """
            SELECT p.orgao,
                   COUNT(*) AS atendidas,
                   ROUND(AVG((p.primeira_acao - o.dt_registro) * 1440), 1) AS minutos_medios,
                   SUM(CASE WHEN o.status = 'RESOLVIDA' THEN 1 ELSE 0 END) AS resolvidas
              FROM (SELECT a.id_ocorrencia, g.orgao, MIN(a.dt_acao) AS primeira_acao
                      FROM cr_acao a
                      JOIN cr_gestor g ON g.id_usuario = a.id_gestor
                     GROUP BY a.id_ocorrencia, g.orgao) p
              JOIN cr_ocorrencia o ON o.id_ocorrencia = p.id_ocorrencia
             GROUP BY p.orgao
             ORDER BY minutos_medios
            """;

    // Exposição: quantos cidadãos de cada modal, quantas rotas eles salvaram e quantas
    // ocorrências ativas estão nas vias dessas rotas.
    private static final String SQL_EXPOSICAO_MODAL = """
            SELECT c.modal,
                   COUNT(DISTINCT c.id_usuario) AS cidadaos,
                   COUNT(DISTINCT ro.id_rota) AS rotas,
                   COUNT(DISTINCT CASE WHEN o.status IN ('ABERTA', 'CONFIRMADA', 'EM_ATENDIMENTO')
                                       THEN o.id_ocorrencia END) AS ocorrencias_ativas
              FROM cr_cidadao c
              LEFT JOIN cr_rota ro ON ro.id_cidadao = c.id_usuario
              LEFT JOIN cr_rota_via rv ON rv.id_rota = ro.id_rota
              LEFT JOIN cr_ocorrencia_via ov ON ov.id_via = rv.id_via
              LEFT JOIN cr_ocorrencia o ON o.id_ocorrencia = ov.id_ocorrencia
             GROUP BY c.modal
            """;

    private static final String SQL_RESUMO = """
            SELECT (SELECT COUNT(*) FROM cr_ocorrencia
                     WHERE status IN ('ABERTA', 'CONFIRMADA', 'EM_ATENDIMENTO')) AS ativas,
                   (SELECT COUNT(*) FROM cr_ocorrencia WHERE status = 'ABERTA') AS aguardando,
                   (SELECT COUNT(*) FROM cr_ocorrencia WHERE status = 'EM_ATENDIMENTO') AS em_atendimento,
                   (SELECT COUNT(*) FROM cr_ocorrencia
                     WHERE status IN ('ABERTA', 'CONFIRMADA', 'EM_ATENDIMENTO') AND gravidade >= 4) AS graves,
                   (SELECT COUNT(*) FROM cr_ocorrencia
                     WHERE status = 'RESOLVIDA' AND dt_registro >= ?) AS resolvidas_30d,
                   (SELECT COUNT(*) FROM cr_cidadao) AS cidadaos,
                   (SELECT COUNT(*) FROM cr_rota) AS rotas
              FROM dual
            """;

    private static final String SQL_TEMPO_MEDIO_GERAL = """
            SELECT ROUND(AVG((p.primeira_acao - o.dt_registro) * 1440), 1) AS minutos_medios
              FROM (SELECT id_ocorrencia, MIN(dt_acao) AS primeira_acao
                      FROM cr_acao
                     GROUP BY id_ocorrencia) p
              JOIN cr_ocorrencia o ON o.id_ocorrencia = p.id_ocorrencia
            """;

    public List<ViaCritica> viasCriticas() {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_VIAS_CRITICAS);
             ResultSet resultado = comando.executeQuery()) {
            List<ViaCritica> linhas = new ArrayList<>();
            while (resultado.next()) {
                linhas.add(new ViaCritica(resultado.getLong("id_via"), resultado.getString("via"),
                        resultado.getString("regiao"), resultado.getInt("total"), resultado.getInt("ativas"),
                        resultado.getDouble("gravidade_media"), resultado.getInt("gravidade_maxima")));
            }
            return linhas;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "gerar o relatório de vias críticas");
        }
    }

    public List<OcorrenciasPorRegiao> ocorrenciasPorRegiao() {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_POR_REGIAO);
             ResultSet resultado = comando.executeQuery()) {
            List<OcorrenciasPorRegiao> linhas = new ArrayList<>();
            while (resultado.next()) {
                linhas.add(new OcorrenciasPorRegiao(resultado.getString("regiao"),
                        TipoOcorrencia.valueOf(resultado.getString("tipo")),
                        resultado.getInt("total"), resultado.getInt("ativas")));
            }
            return linhas;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "gerar o relatório por região");
        }
    }

    public List<TempoRespostaOrgao> tempoRespostaPorOrgao() {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_TEMPO_RESPOSTA);
             ResultSet resultado = comando.executeQuery()) {
            List<TempoRespostaOrgao> linhas = new ArrayList<>();
            while (resultado.next()) {
                linhas.add(new TempoRespostaOrgao(Orgao.valueOf(resultado.getString("orgao")),
                        resultado.getInt("atendidas"), resultado.getDouble("minutos_medios"),
                        resultado.getInt("resolvidas")));
            }
            return linhas;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "gerar o relatório de tempo de resposta");
        }
    }

    /** Contagens por modal. O risco médio é preenchido depois, na RelatorioService. */
    public List<ExposicaoModal> exposicaoPorModal() {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_EXPOSICAO_MODAL);
             ResultSet resultado = comando.executeQuery()) {
            List<ExposicaoModal> linhas = new ArrayList<>();
            while (resultado.next()) {
                linhas.add(new ExposicaoModal(Modal.valueOf(resultado.getString("modal")),
                        resultado.getInt("cidadaos"), resultado.getInt("rotas"),
                        resultado.getInt("ocorrencias_ativas"), 0));
            }
            return linhas;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "gerar o relatório de exposição por modal");
        }
    }

    public ResumoPainel resumo() {
        try (Connection conexao = OracleConnectionFactory.abrirConexao()) {
            Double tempoMedio;
            try (PreparedStatement comando = conexao.prepareStatement(SQL_TEMPO_MEDIO_GERAL);
                 ResultSet resultado = comando.executeQuery()) {
                resultado.next();
                double valor = resultado.getDouble("minutos_medios");
                tempoMedio = resultado.wasNull() ? null : valor;
            }
            try (PreparedStatement comando = conexao.prepareStatement(SQL_RESUMO)) {
                comando.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(30)));
                try (ResultSet resultado = comando.executeQuery()) {
                    resultado.next();
                    return new ResumoPainel(resultado.getInt("ativas"), resultado.getInt("aguardando"),
                            resultado.getInt("em_atendimento"), resultado.getInt("graves"),
                            resultado.getInt("resolvidas_30d"), tempoMedio,
                            resultado.getInt("cidadaos"), resultado.getInt("rotas"));
                }
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "montar o resumo do painel");
        }
    }
}
