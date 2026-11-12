package br.com.citizenlife.api.controller;

import br.com.citizenlife.api.Conversor;
import br.com.citizenlife.api.CorpoJson;
import br.com.citizenlife.api.Requisicao;
import br.com.citizenlife.api.Resposta;
import br.com.citizenlife.api.Roteador;
import br.com.citizenlife.service.AutenticacaoService;

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
        return Resposta.ok(Conversor.cidadao(
                autenticacaoService.entrar(corpo.texto("email"), corpo.texto("senha"))));
    }

    /** GET /api/auth/sessao: devolve o cidadão do cabeçalho X-Usuario-Id, se ele ainda existir. */
    private Resposta sessao(Requisicao requisicao) {
        return Resposta.ok(Conversor.cidadao(autenticacaoService.exigirCidadao(requisicao.idUsuario())));
    }
}
