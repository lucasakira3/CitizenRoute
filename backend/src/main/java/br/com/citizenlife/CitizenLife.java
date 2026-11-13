package br.com.citizenlife;

import br.com.citizenlife.api.Servidor;
import br.com.citizenlife.dao.CidadaoDao;
import br.com.citizenlife.dao.Conexao;
import br.com.citizenlife.dao.EnderecoDao;
import br.com.citizenlife.dao.FeiraDao;
import br.com.citizenlife.dao.OcorrenciaDao;
import br.com.citizenlife.dao.RotaDao;
import br.com.citizenlife.service.CidadaoService;
import br.com.citizenlife.service.EnderecoService;
import br.com.citizenlife.service.FeiraService;
import br.com.citizenlife.service.OcorrenciaService;
import br.com.citizenlife.service.RotaService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Ponto de entrada: monta as camadas (DAO → Service → Servidor) e sobe a API na porta 8080,
 * usando o servidor HTTP que já vem no JDK.
 */
public final class CitizenLife {

    private CitizenLife() {
    }

    public static void main(String[] args) throws IOException {
        int porta = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        RotaDao rotaDao = new RotaDao();
        CidadaoService cidadaoService = new CidadaoService(new CidadaoDao());
        RotaService rotaService = new RotaService(rotaDao, cidadaoService);
        EnderecoService enderecoService = new EnderecoService(new EnderecoDao(), cidadaoService);
        FeiraService feiraService = new FeiraService(new FeiraDao(), cidadaoService);
        OcorrenciaService ocorrenciaService = new OcorrenciaService(new OcorrenciaDao(), rotaDao);

        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        servidor.createContext("/api", new Servidor(cidadaoService, rotaService, enderecoService, feiraService, ocorrenciaService));
        servidor.setExecutor(Executors.newFixedThreadPool(8));
        servidor.start();

        System.out.println("Citizen Life API no ar em http://127.0.0.1:" + porta + "/api");
        System.out.println(Conexao.testar());
    }
}
