package br.com.citizenroute.api.controller;

import br.com.citizenroute.api.Conversor;
import br.com.citizenroute.api.Requisicao;
import br.com.citizenroute.api.Resposta;
import br.com.citizenroute.api.Roteador;
import br.com.citizenroute.service.RegiaoService;

/** CRUD de regiões: /api/regioes. */
public class RegiaoController {

    private final RegiaoService regiaoService;

    public RegiaoController(RegiaoService regiaoService) {
        this.regiaoService = regiaoService;
    }

    public void registrar(Roteador roteador) {
        roteador.get("/api/regioes", this::listar)
                .get("/api/regioes/{id}", this::buscar)
                .post("/api/regioes", this::cadastrar)
                .put("/api/regioes/{id}", this::atualizar)
                .delete("/api/regioes/{id}", this::excluir);
    }

    private Resposta listar(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(regiaoService.listar(), Conversor::regiao));
    }

    private Resposta buscar(Requisicao requisicao) {
        return Resposta.ok(Conversor.regiao(regiaoService.buscar(requisicao.id("id"))));
    }

    private Resposta cadastrar(Requisicao requisicao) {
        return Resposta.criado(Conversor.regiao(
                regiaoService.cadastrar(requisicao.corpo().texto("nome"), requisicao.idUsuario())));
    }

    private Resposta atualizar(Requisicao requisicao) {
        return Resposta.ok(Conversor.regiao(regiaoService.atualizar(requisicao.id("id"),
                requisicao.corpo().texto("nome"), requisicao.idUsuario())));
    }

    private Resposta excluir(Requisicao requisicao) {
        regiaoService.excluir(requisicao.id("id"), requisicao.idUsuario());
        return Resposta.semConteudo();
    }
}
