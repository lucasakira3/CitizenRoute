package br.com.citizenroute.api.controller;

import br.com.citizenroute.api.Conversor;
import br.com.citizenroute.api.CorpoJson;
import br.com.citizenroute.api.Requisicao;
import br.com.citizenroute.api.Resposta;
import br.com.citizenroute.api.Roteador;
import br.com.citizenroute.model.Alerta;
import br.com.citizenroute.model.CriterioRota;
import br.com.citizenroute.model.Modal;
import br.com.citizenroute.model.PontoRota;
import br.com.citizenroute.model.Rota;
import br.com.citizenroute.service.AlertaService;
import br.com.citizenroute.service.RotaService;

import java.util.List;
import java.util.Map;

/**
 * Rotas do cidadão e avaliação de risco de um caminho.
 * <ul>
 *   <li>/api/cidadaos/{id}/rotas: CRUD das rotas salvas (saída → destino)</li>
 *   <li>GET /api/cidadaos/{id}/rotas/{idRota}: a rota com os alertas atuais para o modal do dono</li>
 *   <li>POST /api/rotas/avaliar: risco de um caminho recém-calculado, antes de salvar</li>
 * </ul>
 *
 * <p>Corpo de criação e edição:
 * {"nome", "criterio", "distanciaMetros", "origem": {"descricao", "x", "y"}, "destino": {...}, "vias": [ids]}</p>
 */
public class RotaController {

    private final RotaService rotaService;
    private final AlertaService alertaService;

    public RotaController(RotaService rotaService, AlertaService alertaService) {
        this.rotaService = rotaService;
        this.alertaService = alertaService;
    }

    public void registrar(Roteador roteador) {
        roteador.get("/api/cidadaos/{id}/rotas", this::listar)
                .get("/api/cidadaos/{id}/rotas/{idRota}", this::detalhar)
                .post("/api/cidadaos/{id}/rotas", this::criar)
                .put("/api/cidadaos/{id}/rotas/{idRota}", this::atualizar)
                .delete("/api/cidadaos/{id}/rotas/{idRota}", this::excluir)
                .post("/api/rotas/avaliar", this::avaliar);
    }

    private Resposta listar(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(
                rotaService.listar(requisicao.id("id"), requisicao.idUsuario()), Conversor::rota));
    }

    private Resposta detalhar(Requisicao requisicao) {
        Rota rota = rotaService.buscar(requisicao.id("id"), requisicao.id("idRota"), requisicao.idUsuario());
        Map<String, Object> corpo = Conversor.rota(rota);
        corpo.put("alertas", Conversor.lista(alertaService.alertasDaRota(rota), Conversor::alerta));
        return Resposta.ok(corpo);
    }

    private Resposta criar(Requisicao requisicao) {
        CorpoJson corpo = requisicao.corpo();
        Rota criada = rotaService.criar(requisicao.id("id"), lerRota(corpo), corpo.listaDeNumeros("vias"),
                requisicao.idUsuario());
        return Resposta.criado(Conversor.rota(criada));
    }

    private Resposta atualizar(Requisicao requisicao) {
        CorpoJson corpo = requisicao.corpo();
        Rota rota = rotaService.atualizar(requisicao.id("id"), requisicao.id("idRota"), lerRota(corpo),
                corpo.listaDeNumeros("vias"), requisicao.idUsuario());
        return Resposta.ok(Conversor.rota(rota));
    }

    private Resposta excluir(Requisicao requisicao) {
        rotaService.excluir(requisicao.id("id"), requisicao.id("idRota"), requisicao.idUsuario());
        return Resposta.semConteudo();
    }

    /** POST com {"modal": "MOTO", "vias": [3, 8, 12]}. Não exige login: qualquer pessoa pode planejar. */
    private Resposta avaliar(Requisicao requisicao) {
        CorpoJson corpo = requisicao.corpo();
        List<Alerta> alertas = alertaService.avaliarCaminho(corpo.listaDeNumeros("vias"),
                corpo.opcao("modal", Modal.class));
        return Resposta.ok(Conversor.lista(alertas, Conversor::alerta));
    }

    /** Monta a rota a partir do JSON; a validação de conteúdo fica na RotaService. */
    private Rota lerRota(CorpoJson corpo) {
        Integer distancia = corpo.inteiro("distanciaMetros");
        return new Rota(null, null, corpo.texto("nome"), lerPonto(corpo.objeto("origem")),
                lerPonto(corpo.objeto("destino")), corpo.opcao("criterio", CriterioRota.class),
                distancia == null ? 0 : distancia);
    }

    private PontoRota lerPonto(CorpoJson ponto) {
        if (ponto == null) {
            return null;
        }
        Double x = ponto.decimal("x");
        Double y = ponto.decimal("y");
        return new PontoRota(ponto.texto("descricao"), x == null ? -1 : x, y == null ? -1 : y);
    }
}
