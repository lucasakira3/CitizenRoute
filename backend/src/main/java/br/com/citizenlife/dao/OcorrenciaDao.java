package br.com.citizenlife.dao;

import br.com.citizenlife.model.Ocorrencia;
import br.com.citizenlife.model.Via;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Consulta a tabela CL_OCORRENCIA, sempre com a via junto (JOIN com CL_VIA). */
public class OcorrenciaDao {

    public List<Ocorrencia> listar() {
        String sql = """
                SELECT o.id_ocorrencia, o.titulo, o.descricao, o.tipo, o.gravidade, o.dt_registro,
                       v.id_via, v.nome AS via_nome, v.zona
                  FROM cl_ocorrencia o
                  JOIN cl_via v ON v.id_via = o.id_via
                 ORDER BY o.dt_registro DESC""";
        try (Connection conexao = Conexao.abrir();
             PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {
            List<Ocorrencia> ocorrencias = new ArrayList<>();
            while (resultado.next()) {
                Ocorrencia ocorrencia = new Ocorrencia();
                ocorrencia.setId(resultado.getLong("id_ocorrencia"));
                ocorrencia.setTitulo(resultado.getString("titulo"));
                ocorrencia.setDescricao(resultado.getString("descricao"));
                ocorrencia.setTipo(resultado.getString("tipo"));
                ocorrencia.setGravidade(resultado.getInt("gravidade"));
                ocorrencia.setDataRegistro(Conexao.data(resultado, "dt_registro"));
                ocorrencia.setVia(new Via(resultado.getLong("id_via"), resultado.getString("via_nome"),
                        resultado.getString("zona")));
                ocorrencias.add(ocorrencia);
            }
            return ocorrencias;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "listar as ocorrências");
        }
    }
}
