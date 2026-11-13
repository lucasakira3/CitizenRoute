package br.com.citizenlife.dao;

import br.com.citizenlife.model.Feira;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD da tabela CL_FEIRA e da associação CL_CIDADAO_FEIRA. */
public class FeiraDao implements CrudDao<Feira> {

    private static final String SELECT =
            "SELECT f.id_feira, f.nome, f.rua, f.zona, f.dia_semana, f.horario FROM cl_feira f";

    @Override
    public Feira inserir(Feira feira) {
        String sql = "INSERT INTO cl_feira (nome, rua, zona, dia_semana, horario) VALUES (?, ?, ?, ?, ?)";
        try (Connection conexao = Conexao.abrir();
             PreparedStatement comando = conexao.prepareStatement(sql, new String[]{"ID_FEIRA"})) {
            comando.setString(1, feira.getNome());
            comando.setString(2, feira.getRua());
            comando.setString(3, feira.getZona());
            comando.setString(4, feira.getDiaSemana());
            comando.setString(5, feira.getHorario());
            comando.executeUpdate();
            feira.setId(Conexao.idGerado(comando));
            return feira;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "salvar a feira");
        }
    }

    @Override
    public List<Feira> listar() {
        return consultar(SELECT + " ORDER BY zona, rua, dia_semana", null);
    }

    public List<Feira> listarPorZonaEDia(String zona, String dia) {
        StringBuilder sql = new StringBuilder(SELECT);
        List<Object> parametros = new ArrayList<>();
        if (zona != null && !zona.isBlank()) {
            sql.append(" WHERE f.zona = ?");
            parametros.add(zona);
        }
        if (dia != null && !dia.isBlank()) {
            if (!parametros.isEmpty()) {
                sql.append(" AND");
            } else {
                sql.append(" WHERE");
            }
            sql.append(" f.dia_semana = ?");
            parametros.add(dia);
        }
        sql.append(" ORDER BY f.zona, f.rua");
        return consultar(sql.toString(), parametros.toArray());
    }

    public List<Feira> listarFavoritasPorCidadao(long idCidadao) {
        String sql = SELECT + " JOIN cl_cidadao_feira cf ON cf.id_feira = f.id_feira WHERE cf.id_cidadao = ? "
                + "ORDER BY f.zona, f.rua, f.dia_semana";
        return consultar(sql, idCidadao);
    }

    public Optional<Feira> buscarHojePorCidadao(long idCidadao, String diaSemana) {
        String sql = SELECT + " JOIN cl_cidadao_feira cf ON cf.id_feira = f.id_feira "
                + "WHERE cf.id_cidadao = ? AND f.dia_semana = ? ORDER BY f.rua";
        return consultar(sql, new Object[]{idCidadao, diaSemana}).stream().findFirst();
    }

    @Override
    public Optional<Feira> buscarPorId(long id) {
        return consultar(SELECT + " WHERE f.id_feira = ?", id).stream().findFirst();
    }

    @Override
    public boolean atualizar(Feira feira) {
        String sql = "UPDATE cl_feira SET nome = ?, rua = ?, zona = ?, dia_semana = ?, horario = ? WHERE id_feira = ?";
        try (Connection conexao = Conexao.abrir();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, feira.getNome());
            comando.setString(2, feira.getRua());
            comando.setString(3, feira.getZona());
            comando.setString(4, feira.getDiaSemana());
            comando.setString(5, feira.getHorario());
            comando.setLong(6, feira.getId());
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "atualizar a feira");
        }
    }

    @Override
    public boolean excluir(long id) {
        try (Connection conexao = Conexao.abrir();
             PreparedStatement comando = conexao.prepareStatement("DELETE FROM cl_feira WHERE id_feira = ?")) {
            comando.setLong(1, id);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "excluir a feira");
        }
    }

    public void favoritar(long idCidadao, long idFeira) {
        String sql = "INSERT INTO cl_cidadao_feira (id_cidadao, id_feira) VALUES (?, ?)";
        try (Connection conexao = Conexao.abrir();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, idCidadao);
            comando.setLong(2, idFeira);
            comando.executeUpdate();
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "salvar a feira favorita");
        }
    }

    public boolean desfavoritar(long idCidadao, long idFeira) {
        try (Connection conexao = Conexao.abrir();
             PreparedStatement comando = conexao.prepareStatement(
                     "DELETE FROM cl_cidadao_feira WHERE id_cidadao = ? AND id_feira = ?")) {
            comando.setLong(1, idCidadao);
            comando.setLong(2, idFeira);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "remover a feira favorita");
        }
    }

    private List<Feira> consultar(String sql, Object... parametros) {
        try (Connection conexao = Conexao.abrir();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                comando.setObject(i + 1, parametros[i]);
            }
            List<Feira> feiras = new ArrayList<>();
            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    Feira feira = new Feira();
                    feira.setId(resultado.getLong("id_feira"));
                    feira.setNome(resultado.getString("nome"));
                    feira.setRua(resultado.getString("rua"));
                    feira.setZona(resultado.getString("zona"));
                    feira.setDiaSemana(resultado.getString("dia_semana"));
                    feira.setHorario(resultado.getString("horario"));
                    feiras.add(feira);
                }
            }
            return feiras;
        } catch (SQLException e) {
            throw Conexao.traduzir(e, "consultar as feiras");
        }
    }
}
