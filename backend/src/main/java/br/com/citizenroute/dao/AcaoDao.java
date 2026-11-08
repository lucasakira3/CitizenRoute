package br.com.citizenroute.dao;

import br.com.citizenroute.config.OracleConnectionFactory;
import br.com.citizenroute.model.Acao;
import br.com.citizenroute.model.Gestor;
import br.com.citizenroute.model.Orgao;
import br.com.citizenroute.model.StatusOcorrencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acesso à tabela CR_ACAO, trazendo o gestor e o órgão dele (JOIN). */
public class AcaoDao implements CrudDao<Acao> {

    private static final String SQL_SELECT = """
            SELECT a.id_acao, a.id_ocorrencia, a.descricao, a.status_novo, a.dt_acao,
                   g.id_usuario AS id_gestor, u.nome AS gestor_nome, g.orgao, g.matricula
              FROM cr_acao a
              JOIN cr_gestor g ON g.id_usuario = a.id_gestor
              JOIN cr_usuario u ON u.id_usuario = g.id_usuario
            """;
    private static final String SQL_INSERIR = """
            INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String SQL_ATUALIZAR = "UPDATE cr_acao SET descricao = ? WHERE id_acao = ?";
    private static final String SQL_EXCLUIR = "DELETE FROM cr_acao WHERE id_acao = ?";

    @Override
    public Acao inserir(Acao acao) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao()) {
            return inserir(conexao, acao);
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "registrar a ação");
        }
    }

    /** Versão que usa uma conexão já aberta, para participar da transação de outro DAO. */
    Acao inserir(Connection conexao, Acao acao) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR, new String[]{"ID_ACAO"})) {
            comando.setLong(1, acao.getIdOcorrencia());
            comando.setLong(2, acao.getGestor().getId());
            comando.setString(3, acao.getDescricao());
            if (acao.getStatusNovo() == null) {
                comando.setNull(4, Types.VARCHAR);
            } else {
                comando.setString(4, acao.getStatusNovo().name());
            }
            comando.setTimestamp(5, JdbcUtil.paraTimestamp(acao.getDataAcao()));
            comando.executeUpdate();
            acao.setId(JdbcUtil.idGerado(comando));
            return acao;
        }
    }

    @Override
    public List<Acao> listar() {
        return consultar(SQL_SELECT + " ORDER BY a.dt_acao DESC, a.id_acao DESC", null, "listar as ações");
    }

    /** Histórico de uma ocorrência, do mais antigo para o mais recente. */
    public List<Acao> listarPorOcorrencia(long idOcorrencia) {
        return consultar(SQL_SELECT + " WHERE a.id_ocorrencia = ? ORDER BY a.dt_acao, a.id_acao",
                idOcorrencia, "listar as ações da ocorrência");
    }

    @Override
    public Optional<Acao> buscarPorId(long id) {
        List<Acao> acoes = consultar(SQL_SELECT + " WHERE a.id_acao = ?", id, "buscar a ação");
        return acoes.stream().findFirst();
    }

    /** Só a descrição pode ser editada: o status e o autor fazem parte do histórico. */
    @Override
    public boolean atualizar(Acao acao) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_ATUALIZAR)) {
            comando.setString(1, acao.getDescricao());
            comando.setLong(2, acao.getId());
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "atualizar a ação");
        }
    }

    @Override
    public boolean excluir(long id) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_EXCLUIR)) {
            comando.setLong(1, id);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "excluir a ação");
        }
    }

    private List<Acao> consultar(String sql, Long parametro, String contexto) {
        try (Connection conexao = OracleConnectionFactory.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            if (parametro != null) {
                comando.setLong(1, parametro);
            }
            try (ResultSet resultado = comando.executeQuery()) {
                List<Acao> acoes = new ArrayList<>();
                while (resultado.next()) {
                    acoes.add(mapear(resultado));
                }
                return acoes;
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, contexto);
        }
    }

    private Acao mapear(ResultSet resultado) throws SQLException {
        Gestor gestor = new Gestor(resultado.getLong("id_gestor"), resultado.getString("gestor_nome"), null,
                Orgao.valueOf(resultado.getString("orgao")), resultado.getString("matricula"));
        String status = resultado.getString("status_novo");

        Acao acao = new Acao(resultado.getLong("id_ocorrencia"), gestor, resultado.getString("descricao"),
                status == null ? null : StatusOcorrencia.valueOf(status));
        acao.setId(resultado.getLong("id_acao"));
        acao.setDataAcao(JdbcUtil.lerData(resultado, "dt_acao"));
        return acao;
    }
}
