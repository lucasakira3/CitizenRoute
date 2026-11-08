package br.com.citizenroute.api;

import br.com.citizenroute.exception.AcessoNegadoException;
import br.com.citizenroute.exception.PersistenciaException;
import br.com.citizenroute.exception.RecursoNaoEncontradoException;
import br.com.citizenroute.exception.RegraNegocioException;
import br.com.citizenroute.exception.ValidacaoException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recebe todas as requisições de /api, encontra a rota certa e devolve JSON.
 *
 * <p>É também o único lugar que transforma exceções em respostas HTTP.
 * Assim nenhum erro derruba o servidor e toda falha chega ao usuário com uma mensagem clara.</p>
 */
public final class Roteador implements HttpHandler {

    /** O código que trata uma rota: recebe a requisição e devolve a resposta. */
    @FunctionalInterface
    public interface Manipulador {
        Resposta tratar(Requisicao requisicao);
    }

    private record Rota(String metodo, Pattern padrao, List<String> nomes, Manipulador manipulador) {
    }

    private static final Pattern PARAMETRO = Pattern.compile("\\{(\\w+)}");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final List<Rota> rotas = new ArrayList<>();

    public Roteador get(String caminho, Manipulador manipulador) {
        return adicionar("GET", caminho, manipulador);
    }

    public Roteador post(String caminho, Manipulador manipulador) {
        return adicionar("POST", caminho, manipulador);
    }

    public Roteador put(String caminho, Manipulador manipulador) {
        return adicionar("PUT", caminho, manipulador);
    }

    public Roteador patch(String caminho, Manipulador manipulador) {
        return adicionar("PATCH", caminho, manipulador);
    }

    public Roteador delete(String caminho, Manipulador manipulador) {
        return adicionar("DELETE", caminho, manipulador);
    }

    /** Transforma "/api/vias/{id}" na expressão regular "^/api/vias/([^/]+)$". */
    private Roteador adicionar(String metodo, String caminho, Manipulador manipulador) {
        List<String> nomes = new ArrayList<>();
        Matcher encontrados = PARAMETRO.matcher(caminho);
        StringBuilder regex = new StringBuilder("^");
        int ultimo = 0;
        while (encontrados.find()) {
            regex.append(Pattern.quote(caminho.substring(ultimo, encontrados.start()))).append("([^/]+)");
            nomes.add(encontrados.group(1));
            ultimo = encontrados.end();
        }
        regex.append(Pattern.quote(caminho.substring(ultimo))).append("/?$");
        rotas.add(new Rota(metodo, Pattern.compile(regex.toString()), nomes, manipulador));
        return this;
    }

    @Override
    public void handle(HttpExchange troca) throws IOException {
        long inicio = System.nanoTime();
        Resposta resposta;
        try {
            if ("OPTIONS".equals(troca.getRequestMethod())) {
                resposta = Resposta.semConteudo();
            } else {
                resposta = despachar(troca);
            }
        } catch (ValidacaoException e) {
            resposta = erro(400, e.getMessage(), e.getErros());
        } catch (AcessoNegadoException e) {
            resposta = erro(e.isNaoAutenticado() ? 401 : 403, e.getMessage(), null);
        } catch (RecursoNaoEncontradoException e) {
            resposta = erro(404, e.getMessage(), null);
        } catch (RegraNegocioException e) {
            resposta = erro(409, e.getMessage(), null);
        } catch (PersistenciaException e) {
            registrarFalha(troca, e);
            int status = e.isBancoIndisponivel() ? 503 : e.isConflitoDeDados() ? 409 : 500;
            resposta = erro(status, e.getMessage(), null);
        } catch (RuntimeException e) {
            registrarFalha(troca, e);
            resposta = erro(500, "Erro inesperado no servidor. Tente novamente em instantes.", null);
        }

        enviar(troca, resposta);
        long milissegundos = (System.nanoTime() - inicio) / 1_000_000;
        System.out.printf("%s %-6s %-45s %d (%d ms)%n", LocalTime.now().format(HORA), troca.getRequestMethod(),
                troca.getRequestURI(), resposta.status(), milissegundos);
    }

    private Resposta despachar(HttpExchange troca) {
        String caminho = troca.getRequestURI().getPath();
        String metodo = troca.getRequestMethod();
        boolean caminhoExiste = false;

        for (Rota rota : rotas) {
            Matcher combinacao = rota.padrao().matcher(caminho);
            if (!combinacao.matches()) {
                continue;
            }
            caminhoExiste = true;
            if (!rota.metodo().equals(metodo)) {
                continue;
            }
            Map<String, String> parametros = new HashMap<>();
            for (int i = 0; i < rota.nomes().size(); i++) {
                parametros.put(rota.nomes().get(i), combinacao.group(i + 1));
            }
            return rota.manipulador().tratar(new Requisicao(troca, parametros));
        }
        if (caminhoExiste) {
            return erro(405, "Método " + metodo + " não é aceito em " + caminho + ".", null);
        }
        return erro(404, "Rota não encontrada: " + caminho, null);
    }

    private static Resposta erro(int status, String mensagem, Map<String, String> campos) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("erro", mensagem);
        if (campos != null && !campos.isEmpty()) {
            corpo.put("campos", campos);
        }
        return new Resposta(status, corpo);
    }

    private void enviar(HttpExchange troca, Resposta resposta) {
        try {
            Headers cabecalhos = troca.getResponseHeaders();
            cabecalhos.set("Content-Type", "application/json; charset=utf-8");
            cabecalhos.set("Cache-Control", "no-store");
            liberarOrigemLocal(troca);

            if (resposta.status() == 204 || resposta.corpo() == null) {
                troca.sendResponseHeaders(resposta.status(), -1);
                return;
            }
            byte[] bytes = Json.escrever(resposta.corpo()).getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(resposta.status(), bytes.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(bytes);
            }
        } catch (IOException e) {
            System.err.println("Não foi possível enviar a resposta: " + e.getMessage());
        } finally {
            troca.close();
        }
    }

    /**
     * Em desenvolvimento o Vite repassa as chamadas (proxy) e o CORS nem é usado.
     * Mesmo assim, liberamos origens locais para quem abrir o front em outra porta.
     */
    private void liberarOrigemLocal(HttpExchange troca) {
        String origem = troca.getRequestHeaders().getFirst("Origin");
        if (origem != null && (origem.startsWith("http://localhost:") || origem.startsWith("http://127.0.0.1:"))) {
            Headers cabecalhos = troca.getResponseHeaders();
            cabecalhos.set("Access-Control-Allow-Origin", origem);
            cabecalhos.set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            cabecalhos.set("Access-Control-Allow-Headers", "Content-Type, " + Requisicao.CABECALHO_USUARIO);
            cabecalhos.set("Vary", "Origin");
        }
    }

    private void registrarFalha(HttpExchange troca, Exception e) {
        System.err.println("[ERRO] " + troca.getRequestMethod() + " " + troca.getRequestURI() + ": " + e.getMessage());
        Throwable causa = e.getCause();
        if (causa != null) {
            System.err.println("       causa: " + causa);
        } else if (!(e instanceof PersistenciaException)) {
            e.printStackTrace();
        }
    }
}
