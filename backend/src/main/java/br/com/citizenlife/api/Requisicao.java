package br.com.citizenlife.api;

import br.com.citizenlife.exception.ValidacaoException;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Dados de uma requisição HTTP já separados: parâmetros da rota, da URL, corpo e usuário. */
public final class Requisicao {

    /** Cabeçalho em que o front-end envia o id do usuário logado. */
    public static final String CABECALHO_USUARIO = "X-Usuario-Id";

    private static final int TAMANHO_MAXIMO_CORPO = 64 * 1024;

    private final HttpExchange troca;
    private final Map<String, String> parametrosRota;
    private final Map<String, String> parametrosUrl;
    private CorpoJson corpo;

    Requisicao(HttpExchange troca, Map<String, String> parametrosRota) {
        this.troca = troca;
        this.parametrosRota = parametrosRota;
        this.parametrosUrl = lerParametrosUrl(troca.getRequestURI().getRawQuery());
    }

    /** Parâmetro do caminho, como o {id} de /api/vias/{id}. */
    public long id(String nome) {
        String valor = parametrosRota.get(nome);
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            throw new ValidacaoException("Identificador inválido na URL: \"" + valor + "\".");
        }
    }

    /** Parâmetro da query string (?nome=valor). Nulo quando ausente ou vazio. */
    public String parametro(String nome) {
        String valor = parametrosUrl.get(nome);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public Long parametroLong(String nome) {
        String valor = parametro(nome);
        if (valor == null) {
            return null;
        }
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            throw new ValidacaoException("O filtro \"" + nome + "\" precisa ser um número.");
        }
    }

    public boolean parametroBooleano(String nome) {
        return "true".equalsIgnoreCase(parametro(nome));
    }

    public <E extends Enum<E>> E parametroOpcao(String nome, Class<E> tipo) {
        String valor = parametro(nome);
        if (valor == null) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            String opcoes = Arrays.stream(tipo.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
            throw new ValidacaoException("Filtro \"" + nome + "\" inválido. Use: " + opcoes + ".");
        }
    }

    /** Corpo JSON, lido uma única vez. */
    public CorpoJson corpo() {
        if (corpo == null) {
            corpo = CorpoJson.de(lerCorpo());
        }
        return corpo;
    }

    /** Id do usuário logado, enviado pelo front-end no cabeçalho X-Usuario-Id. */
    public Long idUsuario() {
        String valor = troca.getRequestHeaders().getFirst(CABECALHO_USUARIO);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String lerCorpo() {
        try (InputStream entrada = troca.getRequestBody()) {
            byte[] bytes = entrada.readNBytes(TAMANHO_MAXIMO_CORPO + 1);
            if (bytes.length > TAMANHO_MAXIMO_CORPO) {
                throw new ValidacaoException("O corpo da requisição é grande demais.");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ValidacaoException("Não foi possível ler o corpo da requisição.");
        }
    }

    private static Map<String, String> lerParametrosUrl(String query) {
        Map<String, String> parametros = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return parametros;
        }
        for (String par : query.split("&")) {
            int igual = par.indexOf('=');
            String chave = igual < 0 ? par : par.substring(0, igual);
            String valor = igual < 0 ? "" : par.substring(igual + 1);
            parametros.put(URLDecoder.decode(chave, StandardCharsets.UTF_8),
                    URLDecoder.decode(valor, StandardCharsets.UTF_8));
        }
        return parametros;
    }
}
