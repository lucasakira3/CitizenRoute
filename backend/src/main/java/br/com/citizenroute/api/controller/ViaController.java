package br.com.citizenroute.api.controller;

import br.com.citizenroute.api.Conversor;
import br.com.citizenroute.api.CorpoJson;
import br.com.citizenroute.api.Requisicao;
import br.com.citizenroute.api.Resposta;
import br.com.citizenroute.api.Roteador;
import br.com.citizenroute.service.ViaService;

/** CRUD de vias: /api/vias, com filtros ?regiao= e ?busca=. */
public class ViaController {

    private final ViaService viaService;

    public ViaController(ViaService viaService) {
        this.viaService = viaService;
    }

    public void registrar(Roteador roteador) {
        roteador.get("/api/vias", this::listar)
                .get("/api/vias/{id}", this::buscar)
                .post("/api/vias", this::cadastrar)
                .put("/api/vias/{id}", this::atualizar)
                .delete("/api/vias/{id}", this::excluir);
    }

    private Resposta listar(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(
                viaService.listar(requisicao.parametroLong("regiao"), requisicao.parametro("busca")), Conversor::via));
    }

    private Resposta buscar(Requisicao requisicao) {
        return Resposta.ok(Conversor.via(viaService.buscar(requisicao.id("id"))));
    }

    private Resposta cadastrar(Requisicao requisicao) {
        CorpoJson corpo = requisicao.corpo();
        return Resposta.criado(Conversor.via(
                viaService.cadastrar(corpo.texto("nome"), corpo.longo("idRegiao"), requisicao.idUsuario())));
    }

    private Resposta atualizar(Requisicao requisicao) {
        CorpoJson corpo = requisicao.corpo();
        return Resposta.ok(Conversor.via(viaService.atualizar(requisicao.id("id"), corpo.texto("nome"),
                corpo.longo("idRegiao"), requisicao.idUsuario())));
    }

    private Resposta excluir(Requisicao requisicao) {
        viaService.excluir(requisicao.id("id"), requisicao.idUsuario());
        return Resposta.semConteudo();
    }
}
