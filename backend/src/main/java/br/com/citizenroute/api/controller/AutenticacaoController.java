package br.com.citizenroute.api.controller;

import br.com.citizenroute.api.Conversor;
import br.com.citizenroute.api.CorpoJson;
import br.com.citizenroute.api.Requisicao;
import br.com.citizenroute.api.Resposta;
import br.com.citizenroute.api.Roteador;
import br.com.citizenroute.service.AutenticacaoService;

/** Login e conferência da sessão guardada no navegador. */
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    public AutenticacaoController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    public void registrar(Roteador roteador) {
        roteador.post("/api/auth/login", this::entrar);
        roteador.get("/api/auth/sessao", this::sessao);
    }

    /** POST /api/auth/login com {"email", "senha"}. */
    private Resposta entrar(Requisicao requisicao) {
        CorpoJson corpo = requisicao.corpo();
        return Resposta.ok(Conversor.usuario(
                autenticacaoService.entrar(corpo.texto("email"), corpo.texto("senha"))));
    }

    /** GET /api/auth/sessao: devolve o usuário do cabeçalho X-Usuario-Id, se ele ainda existir. */
    private Resposta sessao(Requisicao requisicao) {
        return Resposta.ok(Conversor.usuario(autenticacaoService.exigirUsuario(requisicao.idUsuario())));
    }
}
