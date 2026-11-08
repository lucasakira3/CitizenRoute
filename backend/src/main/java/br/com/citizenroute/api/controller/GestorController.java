package br.com.citizenroute.api.controller;

import br.com.citizenroute.api.Conversor;
import br.com.citizenroute.api.CorpoJson;
import br.com.citizenroute.api.Requisicao;
import br.com.citizenroute.api.Resposta;
import br.com.citizenroute.api.Roteador;
import br.com.citizenroute.model.Orgao;
import br.com.citizenroute.service.GestorService;

/** CRUD de gestores: /api/gestores. */
public class GestorController {

    private final GestorService gestorService;

    public GestorController(GestorService gestorService) {
        this.gestorService = gestorService;
    }

    public void registrar(Roteador roteador) {
        roteador.get("/api/gestores", this::listar)
                .get("/api/gestores/{id}", this::buscar)
                .post("/api/gestores", this::cadastrar)
                .put("/api/gestores/{id}", this::atualizar)
                .delete("/api/gestores/{id}", this::excluir);
    }

    private Resposta listar(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(gestorService.listar(), Conversor::usuario));
    }

    private Resposta buscar(Requisicao requisicao) {
        return Resposta.ok(Conversor.usuario(gestorService.buscar(requisicao.id("id"))));
    }

    private Resposta cadastrar(Requisicao requisicao) {
        CorpoJson corpo = requisicao.corpo();
        return Resposta.criado(Conversor.usuario(gestorService.cadastrar(corpo.texto("nome"), corpo.texto("email"),
                corpo.opcao("orgao", Orgao.class), corpo.texto("matricula"), corpo.texto("senha"),
                requisicao.idUsuario())));
    }

    private Resposta atualizar(Requisicao requisicao) {
        CorpoJson corpo = requisicao.corpo();
        return Resposta.ok(Conversor.usuario(gestorService.atualizar(requisicao.id("id"), corpo.texto("nome"),
                corpo.texto("email"), corpo.opcao("orgao", Orgao.class), corpo.texto("matricula"),
                corpo.texto("senha"), requisicao.idUsuario())));
    }

    private Resposta excluir(Requisicao requisicao) {
        gestorService.excluir(requisicao.id("id"), requisicao.idUsuario());
        return Resposta.semConteudo();
    }
}
