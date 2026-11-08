package br.com.citizenroute.api.controller;

import br.com.citizenroute.api.Resposta;
import br.com.citizenroute.api.Roteador;
import br.com.citizenroute.service.SaudeService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** GET /api/health: diz se a API está no ar e se o banco está pronto. */
public class SaudeController {

    private final SaudeService saudeService;

    public SaudeController(SaudeService saudeService) {
        this.saudeService = saudeService;
    }

    public void registrar(Roteador roteador) {
        roteador.get("/api/health", requisicao -> verificar());
    }

    private Resposta verificar() {
        Optional<String> problema = saudeService.problemaNoBanco();

        Map<String, Object> banco = new LinkedHashMap<>();
        banco.put("conectado", problema.isEmpty());
        banco.put("configuracao", saudeService.origemConfiguracao());
        banco.put("mensagem", problema.orElse("Oracle conectado e tabelas encontradas."));

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("status", "ok");
        corpo.put("servico", "CitizenRoute API");
        corpo.put("banco", banco);
        return Resposta.ok(corpo);
    }
}
