package br.com.citizenlife.dao;

import br.com.citizenlife.exception.CitizenLifeException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Abre as conexões JDBC com o Oracle e traduz os erros do banco em mensagens claras.
 *
 * <p>As credenciais nunca ficam no código: vêm das variáveis de ambiente ORACLE_JDBC_URL,
 * ORACLE_DB_USER e ORACLE_DB_PASSWORD ou do arquivo backend/db.properties (ignorado pelo Git).</p>
 */
public final class Conexao {

    private static final Properties ARQUIVO = lerArquivo();

    /** Mensagem para cada constraint UNIQUE que o usuário pode violar (o nome vem do script.sql). */
    private static final Map<String, String> REPETIDOS = Map.of(
            "UK_CL_CIDADAO_EMAIL", "Este e-mail já está cadastrado.",
            "UK_CL_ROTA_NOME", "Você já tem uma rota com este nome.",
            "UK_CL_ENDERECO_APELIDO", "Você já tem um endereço com este apelido.",
            "UK_CL_FEIRA_RUA_DIA", "Já existe uma feira para esta rua neste dia.",
            "PK_CL_CIDADAO_FEIRA", "Esta feira já está na sua lista de favoritas.");

    private Conexao() {
    }

    /** Abre uma conexão nova. Os DAOs fecham com try-with-resources. */
    public static Connection abrir() {
        String url = config("ORACLE_JDBC_URL");
        String usuario = config("ORACLE_DB_USER");
        String senha = config("ORACLE_DB_PASSWORD");
        if (url == null || usuario == null || senha == null) {
            throw CitizenLifeException.bancoIndisponivel("Banco não configurado. Crie backend/db.properties a partir "
                    + "de db.properties.example (ou defina ORACLE_JDBC_URL, ORACLE_DB_USER e ORACLE_DB_PASSWORD).", null);
        }
        try {
            DriverManager.setLoginTimeout(10);
            return DriverManager.getConnection(url, usuario, senha);
        } catch (SQLException e) {
            String motivo = e.getErrorCode() == 1017 ? "usuário ou senha do Oracle inválidos."
                    : "o servidor não respondeu. Confira a URL e a sua rede.";
            throw CitizenLifeException.bancoIndisponivel("Não foi possível conectar ao Oracle: " + motivo, e);
        }
    }

    /** Converte uma SQLException em erro da aplicação, com uma mensagem que o usuário entende. */
    public static CitizenLifeException traduzir(SQLException e, String acao) {
        String texto = String.valueOf(e.getMessage()).toUpperCase(Locale.ROOT);
        if (e.getErrorCode() == 1 || "23505".equals(e.getSQLState())) {
            String mensagem = REPETIDOS.entrySet().stream()
                    .filter(item -> texto.contains(item.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse("Já existe um registro com estes dados.");
            return CitizenLifeException.conflito(mensagem, e);
        }
        if (e.getErrorCode() == 942 || texto.contains("NOT FOUND") && texto.contains("TABLE")) {
            return CitizenLifeException.bancoIndisponivel(
                    "As tabelas não foram encontradas. Execute sql/script.sql no seu usuário Oracle.", e);
        }
        return new CitizenLifeException(500, "Erro no banco de dados ao " + acao + ".", e);
    }

    /** Confere se o banco responde; usado na mensagem de quando a API sobe. */
    public static String testar() {
        try (Connection conexao = abrir();
             PreparedStatement comando = conexao.prepareStatement("SELECT COUNT(*) FROM cl_cidadao");
             ResultSet resultado = comando.executeQuery()) {
            resultado.next();
            return "Banco conectado.";
        } catch (SQLException e) {
            return "Atenção: " + traduzir(e, "conferir o banco").getMessage();
        } catch (CitizenLifeException e) {
            return "Atenção: " + e.getMessage();
        }
    }

    /** Id gerado pelo IDENTITY no último INSERT. */
    static long idGerado(PreparedStatement comando) throws SQLException {
        try (ResultSet chaves = comando.getGeneratedKeys()) {
            chaves.next();
            return chaves.getLong(1);
        }
    }

    static LocalDateTime data(ResultSet resultado, String coluna) throws SQLException {
        Timestamp valor = resultado.getTimestamp(coluna);
        return valor == null ? null : valor.toLocalDateTime();
    }

    private static String config(String chave) {
        String valor = System.getenv(chave);
        if (valor == null || valor.isBlank()) {
            valor = ARQUIVO.getProperty(chave);
        }
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static Properties lerArquivo() {
        Properties propriedades = new Properties();
        for (Path caminho : new Path[]{Path.of("db.properties"), Path.of("backend", "db.properties")}) {
            if (Files.isRegularFile(caminho)) {
                try (InputStream entrada = Files.newInputStream(caminho)) {
                    propriedades.load(entrada);
                    break;
                } catch (IOException e) {
                    System.err.println("Não foi possível ler " + caminho + ": " + e.getMessage());
                }
            }
        }
        return propriedades;
    }
}
