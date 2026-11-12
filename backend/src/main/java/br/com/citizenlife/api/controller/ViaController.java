package br.com.citizenlife.api.controller;

import br.com.citizenlife.api.Conversor;
import br.com.citizenlife.api.Requisicao;
import br.com.citizenlife.api.Resposta;
import br.com.citizenlife.api.Roteador;
import br.com.citizenlife.service.ViaService;

/** Vias cadastradas: GET /api/vias e GET /api/vias/{id}. */
public class ViaController {

    private final ViaService viaService;

    public ViaController(ViaService viaService) {
        this.viaService = viaService;
    }

    public void registrar(Roteador roteador) {
        roteador.get("/api/vias", this::listar)
                .get("/api/vias/{id}", this::buscar);
    }

    private Resposta listar(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(viaService.listar(), Conversor::via));
    }

    private Resposta buscar(Requisicao requisicao) {
        return Resposta.ok(Conversor.via(viaService.buscar(requisicao.id("id"))));
    }
}
