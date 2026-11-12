package br.com.citizenlife;

import br.com.citizenlife.api.Roteador;
import br.com.citizenlife.api.controller.AutenticacaoController;
import br.com.citizenlife.api.controller.CidadaoController;
import br.com.citizenlife.api.controller.DominioController;
import br.com.citizenlife.api.controller.EnderecoController;
import br.com.citizenlife.api.controller.OcorrenciaController;
import br.com.citizenlife.api.controller.RotaController;
import br.com.citizenlife.api.controller.SaudeController;
import br.com.citizenlife.api.controller.ViaController;
import br.com.citizenlife.dao.CidadaoDao;
import br.com.citizenlife.dao.EnderecoDao;
import br.com.citizenlife.dao.OcorrenciaDao;
import br.com.citizenlife.dao.RotaDao;
import br.com.citizenlife.dao.SaudeDao;
import br.com.citizenlife.dao.ViaDao;
import br.com.citizenlife.service.AlertaService;
import br.com.citizenlife.service.AutenticacaoService;
import br.com.citizenlife.service.CidadaoService;
import br.com.citizenlife.service.EnderecoService;
import br.com.citizenlife.service.OcorrenciaService;
import br.com.citizenlife.service.RiscoService;
import br.com.citizenlife.service.RotaService;
import br.com.citizenlife.service.SaudeService;
import br.com.citizenlife.service.SenhaService;
import br.com.citizenlife.service.ViaService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ponto de entrada da API.
 *
 * <p>Monta as camadas na ordem DAO → Service → Controller (injeção de dependência feita à mão)
 * e sobe o servidor HTTP que já vem no JDK (com.sun.net.httpserver), sem framework.</p>
 */
public final class CitizenLifeApplication {

    private static final int PORTA_PADRAO = 8080;

    private CitizenLifeApplication() {
    }

    public static void main(String[] args) throws IOException {
        int porta = lerPorta();

        // Camada DAO: acesso ao Oracle via JDBC
        CidadaoDao cidadaoDao = new CidadaoDao();
        ViaDao viaDao = new ViaDao();
        OcorrenciaDao ocorrenciaDao = new OcorrenciaDao();
        RotaDao rotaDao = new RotaDao();
        EnderecoDao enderecoDao = new EnderecoDao();

        // Camada Service: regras de negócio e validações
        SenhaService senhaService = new SenhaService();
        RiscoService riscoService = new RiscoService();
        AutenticacaoService autenticacao = new AutenticacaoService(cidadaoDao, senhaService);
        ViaService viaService = new ViaService(viaDao);
        CidadaoService cidadaoService = new CidadaoService(cidadaoDao, senhaService, autenticacao);
        OcorrenciaService ocorrenciaService = new OcorrenciaService(ocorrenciaDao);
        RotaService rotaService = new RotaService(rotaDao, viaService, cidadaoService, autenticacao);
        EnderecoService enderecoService = new EnderecoService(enderecoDao, autenticacao);
        AlertaService alertaService =
                new AlertaService(ocorrenciaDao, rotaService, cidadaoService, riscoService, autenticacao);
        SaudeService saudeService = new SaudeService(new SaudeDao());

        // Camada de interface: rotas HTTP que o React chama
        Roteador roteador = new Roteador();
        new SaudeController(saudeService).registrar(roteador);
        new DominioController().registrar(roteador);
        new AutenticacaoController(autenticacao).registrar(roteador);
        new ViaController(viaService).registrar(roteador);
        new CidadaoController(cidadaoService, alertaService).registrar(roteador);
        new RotaController(rotaService, alertaService).registrar(roteador);
        new EnderecoController(enderecoService).registrar(roteador);
        new OcorrenciaController(ocorrenciaService, riscoService).registrar(roteador);

        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        servidor.createContext("/api", roteador);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        servidor.setExecutor(executor);
        servidor.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            servidor.stop(1);
            executor.shutdown();
            System.out.println("Citizen Life API encerrada.");
        }));

        System.out.printf("Citizen Life API no ar em http://127.0.0.1:%d/api%n", porta);
        Optional<String> problema = saudeService.problemaNoBanco();
        System.out.println(problema.map(texto -> "Atenção: " + texto)
                .orElse("Banco Oracle conectado (" + saudeService.origemConfiguracao() + ")."));
    }

    private static int lerPorta() {
        String valor = System.getenv("PORT");
        if (valor == null || valor.isBlank()) {
            return PORTA_PADRAO;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            System.err.println("PORT inválida (" + valor + "). Usando " + PORTA_PADRAO + ".");
            return PORTA_PADRAO;
        }
    }
}
