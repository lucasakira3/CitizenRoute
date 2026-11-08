package br.com.citizenroute.api.controller;

import br.com.citizenroute.api.Conversor;
import br.com.citizenroute.api.CorpoJson;
import br.com.citizenroute.api.Requisicao;
import br.com.citizenroute.api.Resposta;
import br.com.citizenroute.api.Roteador;
import br.com.citizenroute.model.Modal;
import br.com.citizenroute.service.AlertaService;
import br.com.citizenroute.service.CidadaoService;
import br.com.citizenroute.service.ViaMonitoradaService;

/**
 * Cidadãos, vias monitoradas e alertas.
 * <ul>
 *   <li>/api/cidadaos: CRUD do cadastro</li>
 *   <li>/api/cidadaos/{id}/vias-monitoradas: CRUD dos trajetos salvos (tabela N:N)</li>
 *   <li>/api/cidadaos/{id}/alertas: ocorrências nas vias do cidadão, com risco para o modal dele</li>
 * </ul>
 */
public class CidadaoController {

    private final CidadaoService cidadaoService;
    private final ViaMonitoradaService viaMonitoradaService;
    private final AlertaService alertaService;

    public CidadaoController(CidadaoService cidadaoService, ViaMonitoradaService viaMonitoradaService,
                             AlertaService alertaService) {
        this.cidadaoService = cidadaoService;
        this.viaMonitoradaService = viaMonitoradaService;
        this.alertaService = alertaService;
    }

    public void registrar(Roteador roteador) {
        roteador.get("/api/cidadaos", this::listar)
                .get("/api/cidadaos/{id}", this::buscar)
                .post("/api/cidadaos", this::cadastrar)
                .put("/api/cidadaos/{id}", this::atualizar)
                .delete("/api/cidadaos/{id}", this::excluir)
                .get("/api/cidadaos/{id}/vias-monitoradas", this::listarVias)
                .post("/api/cidadaos/{id}/vias-monitoradas", this::adicionarVia)
                .put("/api/cidadaos/{id}/vias-monitoradas/{idVia}", this::atualizarVia)
                .delete("/api/cidadaos/{id}/vias-monitoradas/{idVia}", this::removerVia)
                .get("/api/cidadaos/{id}/alertas", this::alertas);
    }

    private Resposta listar(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(cidadaoService.listar(), Conversor::usuario));
    }

    private Resposta buscar(Requisicao requisicao) {
        return Resposta.ok(Conversor.usuario(cidadaoService.buscar(requisicao.id("id"))));
    }

    private Resposta cadastrar(Requisicao requisicao) {
        CorpoJson corpo = requisicao.corpo();
        return Resposta.criado(Conversor.usuario(cidadaoService.cadastrar(corpo.texto("nome"), corpo.texto("email"),
                corpo.opcao("modal", Modal.class), corpo.texto("senha"))));
    }

    private Resposta atualizar(Requisicao requisicao) {
        CorpoJson corpo = requisicao.corpo();
        return Resposta.ok(Conversor.usuario(cidadaoService.atualizar(requisicao.id("id"), corpo.texto("nome"),
                corpo.texto("email"), corpo.opcao("modal", Modal.class), corpo.texto("senha"),
                requisicao.idUsuario())));
    }

    private Resposta excluir(Requisicao requisicao) {
        cidadaoService.excluir(requisicao.id("id"), requisicao.idUsuario());
        return Resposta.semConteudo();
    }

    private Resposta listarVias(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(
                viaMonitoradaService.listar(requisicao.id("id"), requisicao.idUsuario()), Conversor::viaMonitorada));
    }

    private Resposta adicionarVia(Requisicao requisicao) {
        CorpoJson corpo = requisicao.corpo();
        return Resposta.criado(Conversor.viaMonitorada(viaMonitoradaService.adicionar(requisicao.id("id"),
                corpo.longo("idVia"), corpo.texto("apelido"), requisicao.idUsuario())));
    }

    private Resposta atualizarVia(Requisicao requisicao) {
        return Resposta.ok(Conversor.viaMonitorada(viaMonitoradaService.atualizar(requisicao.id("id"),
                requisicao.id("idVia"), requisicao.corpo().texto("apelido"), requisicao.idUsuario())));
    }

    private Resposta removerVia(Requisicao requisicao) {
        viaMonitoradaService.remover(requisicao.id("id"), requisicao.id("idVia"), requisicao.idUsuario());
        return Resposta.semConteudo();
    }

    private Resposta alertas(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(
                alertaService.alertasDoCidadao(requisicao.id("id"), requisicao.idUsuario()), Conversor::alerta));
    }
}
