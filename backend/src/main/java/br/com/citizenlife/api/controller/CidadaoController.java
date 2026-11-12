package br.com.citizenlife.api.controller;

import br.com.citizenlife.api.Conversor;
import br.com.citizenlife.api.CorpoJson;
import br.com.citizenlife.api.Requisicao;
import br.com.citizenlife.api.Resposta;
import br.com.citizenlife.api.Roteador;
import br.com.citizenlife.model.Modal;
import br.com.citizenlife.service.AlertaService;
import br.com.citizenlife.service.CidadaoService;

/**
 * Cidadãos e alertas.
 * <ul>
 *   <li>/api/cidadaos: CRUD do cadastro (cada cidadão só vê e altera a própria conta)</li>
 *   <li>/api/cidadaos/{id}/alertas: ocorrências nas vias das rotas salvas, com risco para o modal dele</li>
 *   <li>as rotas ficam no {@link RotaController}</li>
 * </ul>
 */
public class CidadaoController {

    private final CidadaoService cidadaoService;
    private final AlertaService alertaService;

    public CidadaoController(CidadaoService cidadaoService, AlertaService alertaService) {
        this.cidadaoService = cidadaoService;
        this.alertaService = alertaService;
    }

    public void registrar(Roteador roteador) {
        roteador.get("/api/cidadaos/{id}", this::buscar)
                .post("/api/cidadaos", this::cadastrar)
                .put("/api/cidadaos/{id}", this::atualizar)
                .delete("/api/cidadaos/{id}", this::excluir)
                .get("/api/cidadaos/{id}/alertas", this::alertas);
    }

    private Resposta buscar(Requisicao requisicao) {
        return Resposta.ok(Conversor.cidadao(cidadaoService.buscar(requisicao.id("id"), requisicao.idUsuario())));
    }

    private Resposta cadastrar(Requisicao requisicao) {
        CorpoJson corpo = requisicao.corpo();
        return Resposta.criado(Conversor.cidadao(cidadaoService.cadastrar(corpo.texto("nome"), corpo.texto("email"),
                corpo.opcao("modal", Modal.class), corpo.texto("senha"))));
    }

    private Resposta atualizar(Requisicao requisicao) {
        CorpoJson corpo = requisicao.corpo();
        return Resposta.ok(Conversor.cidadao(cidadaoService.atualizar(requisicao.id("id"), corpo.texto("nome"),
                corpo.texto("email"), corpo.opcao("modal", Modal.class), corpo.texto("senha"),
                requisicao.idUsuario())));
    }

    private Resposta excluir(Requisicao requisicao) {
        cidadaoService.excluir(requisicao.id("id"), requisicao.idUsuario());
        return Resposta.semConteudo();
    }

    private Resposta alertas(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(
                alertaService.alertasDoCidadao(requisicao.id("id"), requisicao.idUsuario()), Conversor::alerta));
    }
}
