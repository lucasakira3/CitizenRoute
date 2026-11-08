package br.com.citizenroute.config;

import br.com.citizenroute.dao.TradutorErroSql;
import br.com.citizenroute.exception.PersistenciaException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * Abre conexões JDBC com o Oracle.
 *
 * <p>As credenciais nunca ficam no código. A fábrica procura, nesta ordem:</p>
 * <ol>
 *   <li>variáveis de ambiente ORACLE_JDBC_URL, ORACLE_DB_USER e ORACLE_DB_PASSWORD;</li>
 *   <li>o arquivo db.properties (copiado de db.properties.example e ignorado pelo Git).</li>
 * </ol>
 */
public final class OracleConnectionFactory {

    public static final String CHAVE_URL = "ORACLE_JDBC_URL";
    public static final String CHAVE_USUARIO = "ORACLE_DB_USER";
    public static final String CHAVE_SENHA = "ORACLE_DB_PASSWORD";

    private static final List<Path> ARQUIVOS_CONFIGURACAO = List.of(
            Path.of("db.properties"),
            Path.of("backend", "db.properties"));

    private static final Properties ARQUIVO = carregarArquivo();

    private OracleConnectionFactory() {
    }

    /** Abre uma nova conexão. Quem chama deve fechá-la (os DAOs usam try-with-resources). */
    public static Connection abrirConexao() {
        String url = obter(CHAVE_URL);
        String usuario = obter(CHAVE_USUARIO);
        String senha = obter(CHAVE_SENHA);

        if (url == null || usuario == null || senha == null) {
            throw PersistenciaException.indisponivel(
                    "Banco não configurado. Defina " + CHAVE_URL + ", " + CHAVE_USUARIO + " e " + CHAVE_SENHA
                            + " como variáveis de ambiente ou crie backend/db.properties a partir de db.properties.example.",
                    null);
        }

        try {
            DriverManager.setLoginTimeout(10);
            return DriverManager.getConnection(url, usuario, senha);
        } catch (SQLException e) {
            throw TradutorErroSql.traduzirConexao(e);
        }
    }

    /** Indica de onde veio a configuração, sem expor a senha. Usado no endpoint de saúde. */
    public static String descreverOrigem() {
        if (System.getenv(CHAVE_URL) != null) {
            return "variáveis de ambiente";
        }
        if (ARQUIVO.getProperty(CHAVE_URL) != null) {
            return "arquivo db.properties";
        }
        return "não configurado";
    }

    private static String obter(String chave) {
        String valor = System.getenv(chave);
        if (valor == null || valor.isBlank()) {
            valor = ARQUIVO.getProperty(chave);
        }
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static Properties carregarArquivo() {
        Properties propriedades = new Properties();
        for (Path caminho : ARQUIVOS_CONFIGURACAO) {
            if (Files.isRegularFile(caminho)) {
                try (InputStream entrada = Files.newInputStream(caminho)) {
                    propriedades.load(entrada);
                    System.out.println("Configuração do banco lida de " + caminho.toAbsolutePath());
                    break;
                } catch (IOException e) {
                    System.err.println("Não foi possível ler " + caminho + ": " + e.getMessage());
                }
            }
        }
        return propriedades;
    }
}
