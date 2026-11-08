package br.com.citizenroute;

import br.com.citizenroute.api.Roteador;
import br.com.citizenroute.api.controller.AutenticacaoController;
import br.com.citizenroute.api.controller.CidadaoController;
import br.com.citizenroute.api.controller.DominioController;
import br.com.citizenroute.api.controller.GestorController;
import br.com.citizenroute.api.controller.OcorrenciaController;
import br.com.citizenroute.api.controller.RegiaoController;
import br.com.citizenroute.api.controller.RelatorioController;
import br.com.citizenroute.api.controller.SaudeController;
import br.com.citizenroute.api.controller.ViaController;
import br.com.citizenroute.dao.AcaoDao;
import br.com.citizenroute.dao.CidadaoDao;
import br.com.citizenroute.dao.GestorDao;
import br.com.citizenroute.dao.OcorrenciaDao;
import br.com.citizenroute.dao.RegiaoDao;
import br.com.citizenroute.dao.RelatorioDao;
import br.com.citizenroute.dao.SaudeDao;
import br.com.citizenroute.dao.UsuarioDao;
import br.com.citizenroute.dao.ViaDao;
import br.com.citizenroute.dao.ViaMonitoradaDao;
import br.com.citizenroute.service.AcaoService;
import br.com.citizenroute.service.AlertaService;
import br.com.citizenroute.service.AutenticacaoService;
import br.com.citizenroute.service.CidadaoService;
import br.com.citizenroute.service.GestorService;
import br.com.citizenroute.service.OcorrenciaService;
import br.com.citizenroute.service.RegiaoService;
import br.com.citizenroute.service.RelatorioService;
import br.com.citizenroute.service.RiscoService;
import br.com.citizenroute.service.SaudeService;
import br.com.citizenroute.service.SenhaService;
import br.com.citizenroute.service.ViaMonitoradaService;
import br.com.citizenroute.service.ViaService;
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
public final class CitizenRouteApplication {

    private static final int PORTA_PADRAO = 8080;

    private CitizenRouteApplication() {
    }

    public static void main(String[] args) throws IOException {
        int porta = lerPorta();

        // Camada DAO: acesso ao Oracle via JDBC
        UsuarioDao usuarioDao = new UsuarioDao();
        CidadaoDao cidadaoDao = new CidadaoDao();
        GestorDao gestorDao = new GestorDao();
        RegiaoDao regiaoDao = new RegiaoDao();
        ViaDao viaDao = new ViaDao();
        AcaoDao acaoDao = new AcaoDao();
        OcorrenciaDao ocorrenciaDao = new OcorrenciaDao(acaoDao);
        ViaMonitoradaDao viaMonitoradaDao = new ViaMonitoradaDao();
        RelatorioDao relatorioDao = new RelatorioDao();

        // Camada Service: regras de negócio e validações
        SenhaService senhaService = new SenhaService();
        RiscoService riscoService = new RiscoService();
        AutenticacaoService autenticacao = new AutenticacaoService(usuarioDao, senhaService);
        RegiaoService regiaoService = new RegiaoService(regiaoDao, autenticacao);
        ViaService viaService = new ViaService(viaDao, regiaoService, autenticacao);
        CidadaoService cidadaoService = new CidadaoService(cidadaoDao, usuarioDao, senhaService, autenticacao);
        GestorService gestorService = new GestorService(gestorDao, usuarioDao, senhaService, autenticacao);
        OcorrenciaService ocorrenciaService = new OcorrenciaService(ocorrenciaDao, viaDao, autenticacao);
        AcaoService acaoService = new AcaoService(acaoDao, ocorrenciaService, autenticacao);
        ViaMonitoradaService viaMonitoradaService =
                new ViaMonitoradaService(viaMonitoradaDao, viaService, cidadaoService, autenticacao);
        AlertaService alertaService =
                new AlertaService(ocorrenciaDao, viaMonitoradaDao, cidadaoService, riscoService, autenticacao);
        RelatorioService relatorioService = new RelatorioService(relatorioDao, ocorrenciaDao, riscoService);
        SaudeService saudeService = new SaudeService(new SaudeDao());

        // Camada de interface: rotas HTTP que o React chama
        Roteador roteador = new Roteador();
        new SaudeController(saudeService).registrar(roteador);
        new DominioController().registrar(roteador);
        new AutenticacaoController(autenticacao).registrar(roteador);
        new RegiaoController(regiaoService).registrar(roteador);
        new ViaController(viaService).registrar(roteador);
        new CidadaoController(cidadaoService, viaMonitoradaService, alertaService).registrar(roteador);
        new GestorController(gestorService).registrar(roteador);
        new OcorrenciaController(ocorrenciaService, acaoService, riscoService).registrar(roteador);
        new RelatorioController(relatorioService).registrar(roteador);

        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        servidor.createContext("/api", roteador);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        servidor.setExecutor(executor);
        servidor.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            servidor.stop(1);
            executor.shutdown();
            System.out.println("CitizenRoute API encerrada.");
        }));

        System.out.printf("CitizenRoute API no ar em http://127.0.0.1:%d/api%n", porta);
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
