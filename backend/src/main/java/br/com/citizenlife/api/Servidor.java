package br.com.citizenlife.api;

import br.com.citizenlife.exception.CitizenLifeException;
import br.com.citizenlife.exception.ValidacaoException;
import br.com.citizenlife.model.Cidadao;
import br.com.citizenlife.model.Endereco;
import br.com.citizenlife.model.Ocorrencia;
import br.com.citizenlife.model.Rota;
import br.com.citizenlife.model.Via;
import br.com.citizenlife.service.CidadaoService;
import br.com.citizenlife.service.EnderecoService;
import br.com.citizenlife.service.OcorrenciaService;
import br.com.citizenlife.service.RotaService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A camada de interface: recebe as chamadas HTTP do site (React), chama a service certa e devolve JSON.
 *
 * <p>Todas as rotas da API estão no switch de {@link #rotear}. Os números do caminho viram {id}:
 * "GET /api/cidadaos/3/rotas" é atendido pelo caso "GET /api/cidadaos/{id}/rotas".</p>
 * <p>Qualquer erro vira uma resposta com mensagem clara: o servidor nunca cai.</p>
 */
public class Servidor implements HttpHandler {

    private static final Pattern NUMERO = Pattern.compile("/(\\d+)");

    private final CidadaoService cidadaos;
    private final RotaService rotas;
    private final EnderecoService enderecos;
    private final OcorrenciaService ocorrencias;

    public Servidor(CidadaoService cidadaos, RotaService rotas, EnderecoService enderecos,
                    OcorrenciaService ocorrencias) {
        this.cidadaos = cidadaos;
        this.rotas = rotas;
        this.enderecos = enderecos;
        this.ocorrencias = ocorrencias;
    }

    @Override
    public void handle(HttpExchange troca) throws IOException {
        int status;
        Object corpo;
        try {
            String caminho = troca.getRequestURI().getPath();
            List<Long> ids = new ArrayList<>();
            Matcher numeros = NUMERO.matcher(caminho);
            while (numeros.find()) {
                ids.add(Long.parseLong(numeros.group(1)));
            }
            String rota = troca.getRequestMethod() + " " + NUMERO.matcher(caminho).replaceAll("/{id}");
            String usuario = troca.getRequestHeaders().getFirst("X-Usuario-Id");
            // Cabeçalho ausente ou que não é um número: trata como quem não entrou (401)
            Long ator = usuario == null || !usuario.trim().matches("\\d{1,18}") ? null : Long.valueOf(usuario.trim());

            corpo = rotear(rota, ids, ator, lerCorpo(troca), lerParametros(troca));
            status = corpo == null ? 204 : rota.startsWith("POST /api/cidadaos") ? 201 : 200;
        } catch (CitizenLifeException e) {
            status = e.getStatus();
            Map<String, Object> erro = new LinkedHashMap<>();
            erro.put("erro", e.getMessage());
            if (e instanceof ValidacaoException validacao) {
                erro.put("campos", validacao.getErros());
            }
            corpo = erro;
        } catch (RuntimeException e) {
            e.printStackTrace();
            status = 500;
            corpo = Map.of("erro", "Erro inesperado no servidor. Tente de novo.");
        }
        enviar(troca, status, corpo);
        System.out.printf("%s %s -> %d%n", troca.getRequestMethod(), troca.getRequestURI(), status);
    }

    /** Tabela de rotas da API. Devolve o corpo da resposta (null = 204, sem conteúdo). */
    private Object rotear(String rota, List<Long> ids, Long ator, Map<String, Object> json, Map<String, String> parametros) {
        return switch (rota) {
            // Login e conta
            case "POST /api/auth/login" -> cidadao(cidadaos.entrar(texto(json, "email"), texto(json, "senha")));
            case "GET /api/auth/sessao" -> cidadao(cidadaos.exigirDono(ator, ator == null ? 0 : ator));
            case "POST /api/cidadaos" ->
                    cidadao(cidadaos.cadastrar(texto(json, "nome"), texto(json, "email"), texto(json, "senha")));
            case "PUT /api/cidadaos/{id}" -> cidadao(cidadaos.atualizar(ids.get(0), texto(json, "nome"),
                    texto(json, "email"), texto(json, "modal"), texto(json, "senha"), ator));
            case "DELETE /api/cidadaos/{id}" -> {
                cidadaos.excluir(ids.get(0), ator);
                yield null;
            }

            // Rotas salvas (CRUD)
            case "GET /api/cidadaos/{id}/rotas" -> rotas.listar(ids.get(0), ator).stream().map(this::rota).toList();
            case "GET /api/cidadaos/{id}/rotas/{id}" -> rota(rotas.buscar(ids.get(0), ids.get(1), ator));
            case "POST /api/cidadaos/{id}/rotas" -> rota(rotas.criar(ids.get(0), lerRota(json), idsVias(json), ator));
            case "PUT /api/cidadaos/{id}/rotas/{id}" ->
                    rota(rotas.atualizar(ids.get(0), ids.get(1), lerRota(json), idsVias(json), ator));
            case "DELETE /api/cidadaos/{id}/rotas/{id}" -> {
                rotas.excluir(ids.get(0), ids.get(1), ator);
                yield null;
            }

            // Endereços salvos (CRUD)
            case "GET /api/cidadaos/{id}/enderecos" ->
                    enderecos.listar(ids.get(0), ator).stream().map(this::endereco).toList();
            case "POST /api/cidadaos/{id}/enderecos" -> endereco(enderecos.criar(ids.get(0), lerEndereco(json), ator));
            case "PUT /api/cidadaos/{id}/enderecos/{id}" ->
                    endereco(enderecos.atualizar(ids.get(0), ids.get(1), lerEndereco(json), ator));
            case "DELETE /api/cidadaos/{id}/enderecos/{id}" -> {
                enderecos.excluir(ids.get(0), ids.get(1), ator);
                yield null;
            }

            // Mobilidade: vias, ocorrências com risco e alertas
            case "GET /api/vias" -> rotas.listarVias().stream().map(this::via).toList();
            case "GET /api/ocorrencias" -> {
                String modal = parametros.getOrDefault("modal", Cidadao.MODAL_PADRAO);
                yield ocorrencias.listar(modal).stream().map(o -> ocorrencia(o, modal)).toList();
            }
            case "POST /api/rotas/avaliar" -> {
                String modal = texto(json, "modal");
                yield ocorrencias.noCaminho(idsVias(json), modal).stream().map(o -> ocorrencia(o, modal)).toList();
            }
            case "GET /api/cidadaos/{id}/alertas" -> {
                Cidadao cidadao = cidadaos.exigirDono(ator, ids.get(0));
                yield ocorrencias.alertas(cidadao).stream()
                        .map(o -> ocorrencia(o, cidadao.modalParaRisco())).toList();
            }
            default -> throw CitizenLifeException.naoEncontrado("Caminho não encontrado: " + rota);
        };
    }

    // -------------------------------------------------------- objetos → JSON

    private Map<String, Object> cidadao(Cidadao cidadao) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", cidadao.getId());
        mapa.put("nome", cidadao.getNome());
        mapa.put("email", cidadao.getEmail());
        mapa.put("modal", cidadao.getModal());
        mapa.put("dataCadastro", cidadao.getDataCadastro());
        return mapa;
    }

    private Map<String, Object> via(Via via) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", via.getId());
        mapa.put("nome", via.getNome());
        mapa.put("zona", via.getZona());
        return mapa;
    }

    /** A ocorrência com o risco já calculado para o modal de quem vai ver. */
    private Map<String, Object> ocorrencia(Ocorrencia ocorrencia, String modal) {
        double risco = ocorrencia.calcularRisco(modal);
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", ocorrencia.getId());
        mapa.put("titulo", ocorrencia.getTitulo());
        mapa.put("descricao", ocorrencia.getDescricao());
        mapa.put("tipo", ocorrencia.getTipo());
        mapa.put("gravidade", ocorrencia.getGravidade());
        mapa.put("dataRegistro", ocorrencia.getDataRegistro());
        mapa.put("via", via(ocorrencia.getVia()));
        mapa.put("risco", Map.of("valor", risco, "nivel", Ocorrencia.nivel(risco),
                "alerta", risco >= Ocorrencia.LIMITE_ALERTA));
        return mapa;
    }

    private Map<String, Object> rota(Rota rota) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", rota.getId());
        mapa.put("nome", rota.getNome());
        mapa.put("origem", ponto(rota.getOrigemDescricao(), rota.getOrigemX(), rota.getOrigemY()));
        mapa.put("destino", ponto(rota.getDestinoDescricao(), rota.getDestinoX(), rota.getDestinoY()));
        mapa.put("distanciaMetros", rota.getDistanciaMetros());
        mapa.put("dataCriacao", rota.getDataCriacao());
        mapa.put("vias", rota.getVias().stream().map(this::via).toList());
        return mapa;
    }

    private Map<String, Object> endereco(Endereco endereco) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", endereco.getId());
        mapa.put("apelido", endereco.getApelido());
        mapa.put("tipo", endereco.getTipo());
        mapa.put("local", ponto(endereco.getDescricao(), endereco.getX(), endereco.getY()));
        return mapa;
    }

    private Map<String, Object> ponto(String descricao, double x, double y) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("descricao", descricao);
        mapa.put("x", x);
        mapa.put("y", y);
        return mapa;
    }

    // -------------------------------------------------------- JSON → objetos

    private Rota lerRota(Map<String, Object> json) {
        Map<String, Object> origem = objeto(json, "origem");
        Map<String, Object> destino = objeto(json, "destino");
        Rota rota = new Rota();
        rota.setNome(texto(json, "nome"));
        rota.setOrigemDescricao(texto(origem, "descricao"));
        rota.setOrigemX(numero(origem, "x"));
        rota.setOrigemY(numero(origem, "y"));
        rota.setDestinoDescricao(texto(destino, "descricao"));
        rota.setDestinoX(numero(destino, "x"));
        rota.setDestinoY(numero(destino, "y"));
        rota.setDistanciaMetros((int) numero(json, "distanciaMetros"));
        return rota;
    }

    private Endereco lerEndereco(Map<String, Object> json) {
        Map<String, Object> local = objeto(json, "local");
        Endereco endereco = new Endereco();
        endereco.setApelido(texto(json, "apelido"));
        endereco.setTipo(texto(json, "tipo"));
        endereco.setDescricao(texto(local, "descricao"));
        endereco.setX(numero(local, "x"));
        endereco.setY(numero(local, "y"));
        return endereco;
    }

    private List<Long> idsVias(Map<String, Object> json) {
        List<Long> ids = new ArrayList<>();
        if (json.get("vias") instanceof List<?> lista) {
            for (Object item : lista) {
                if (item instanceof Number numero) {
                    ids.add(numero.longValue());
                }
            }
        }
        return ids;
    }

    private String texto(Map<String, Object> json, String campo) {
        Object valor = json.get(campo);
        return valor == null ? null : valor.toString();
    }

    /** Número do JSON; -1 quando o campo falta, para a service acusar o erro. */
    private double numero(Map<String, Object> json, String campo) {
        return json.get(campo) instanceof Number numero ? numero.doubleValue() : -1;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objeto(Map<String, Object> json, String campo) {
        return json.get(campo) instanceof Map<?, ?> mapa ? (Map<String, Object>) mapa : Map.of();
    }

    // -------------------------------------------------------------------- HTTP

    @SuppressWarnings("unchecked")
    private Map<String, Object> lerCorpo(HttpExchange troca) throws IOException {
        String texto = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (texto.isBlank()) {
            return Map.of();
        }
        if (!(Json.ler(texto) instanceof Map<?, ?> mapa)) {
            throw new ValidacaoException("corpo", "O corpo da requisição precisa ser um objeto JSON.");
        }
        return (Map<String, Object>) mapa;
    }

    private Map<String, String> lerParametros(HttpExchange troca) {
        Map<String, String> parametros = new LinkedHashMap<>();
        String consulta = troca.getRequestURI().getRawQuery();
        if (consulta != null) {
            for (String par : consulta.split("&")) {
                String[] partes = par.split("=", 2);
                if (partes.length == 2) {
                    parametros.put(partes[0], URLDecoder.decode(partes[1], StandardCharsets.UTF_8));
                }
            }
        }
        return parametros;
    }

    private void enviar(HttpExchange troca, int status, Object corpo) throws IOException {
        if (status == 204) {
            troca.sendResponseHeaders(204, -1);
            troca.close();
            return;
        }
        byte[] bytes = Json.escrever(corpo).getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        troca.sendResponseHeaders(status, bytes.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(bytes);
        }
    }
}
