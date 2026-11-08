package br.com.citizenroute.api.controller;

import br.com.citizenroute.api.Conversor;
import br.com.citizenroute.api.CorpoJson;
import br.com.citizenroute.api.Requisicao;
import br.com.citizenroute.api.Resposta;
import br.com.citizenroute.api.Roteador;
import br.com.citizenroute.model.Modal;
import br.com.citizenroute.model.NivelImpacto;
import br.com.citizenroute.model.Ocorrencia;
import br.com.citizenroute.model.OcorrenciaVia;
import br.com.citizenroute.model.Orgao;
import br.com.citizenroute.model.StatusOcorrencia;
import br.com.citizenroute.model.TipoOcorrencia;
import br.com.citizenroute.model.Via;
import br.com.citizenroute.service.AcaoService;
import br.com.citizenroute.service.OcorrenciaService;
import br.com.citizenroute.service.RiscoService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ocorrências e o histórico de ações.
 * <ul>
 *   <li>/api/ocorrencias: CRUD, com filtros ?status, ?tipo, ?orgao, ?regiao, ?via, ?autor, ?ativas e ?modal</li>
 *   <li>PATCH /api/ocorrencias/{id}/status: gestor muda o status e registra a ação</li>
 *   <li>/api/ocorrencias/{id}/acoes e /api/acoes/{id}: andamentos registrados pelos gestores</li>
 * </ul>
 */
public class OcorrenciaController {

    private final OcorrenciaService ocorrenciaService;
    private final AcaoService acaoService;
    private final RiscoService riscoService;

    public OcorrenciaController(OcorrenciaService ocorrenciaService, AcaoService acaoService,
                                RiscoService riscoService) {
        this.ocorrenciaService = ocorrenciaService;
        this.acaoService = acaoService;
        this.riscoService = riscoService;
    }

    public void registrar(Roteador roteador) {
        roteador.get("/api/ocorrencias", this::listar)
                .get("/api/ocorrencias/{id}", this::detalhar)
                .post("/api/ocorrencias", this::registrarOcorrencia)
                .put("/api/ocorrencias/{id}", this::atualizar)
                .delete("/api/ocorrencias/{id}", this::excluir)
                .patch("/api/ocorrencias/{id}/status", this::alterarStatus)
                .get("/api/ocorrencias/{id}/acoes", this::listarAcoes)
                .post("/api/ocorrencias/{id}/acoes", this::registrarAcao)
                .put("/api/acoes/{id}", this::atualizarAcao)
                .delete("/api/acoes/{id}", this::excluirAcao);
    }

    /** Com ?modal=MOTO, cada item vem com o risco calculado para quem anda de moto. */
    private Resposta listar(Requisicao requisicao) {
        List<Ocorrencia> ocorrencias = ocorrenciaService.listar(
                requisicao.parametroOpcao("status", StatusOcorrencia.class),
                requisicao.parametroOpcao("tipo", TipoOcorrencia.class),
                requisicao.parametroOpcao("orgao", Orgao.class),
                requisicao.parametroLong("regiao"),
                requisicao.parametroLong("via"),
                requisicao.parametroLong("autor"),
                requisicao.parametroBooleano("ativas"));
        Modal modal = requisicao.parametroOpcao("modal", Modal.class);

        List<Map<String, Object>> itens = new ArrayList<>();
        for (Ocorrencia ocorrencia : ocorrencias) {
            Map<String, Object> item = Conversor.ocorrencia(ocorrencia);
            if (modal != null) {
                item.put("risco", risco(ocorrencia, modal));
            }
            itens.add(item);
        }
        return Resposta.ok(itens);
    }

    /** Detalhe com histórico, risco para cada modal e próximos status possíveis. */
    private Resposta detalhar(Requisicao requisicao) {
        long id = requisicao.id("id");
        Ocorrencia ocorrencia = ocorrenciaService.buscar(id);
        Map<String, Object> corpo = Conversor.ocorrencia(ocorrencia);

        List<Map<String, Object>> riscos = new ArrayList<>();
        for (Modal modal : Modal.values()) {
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("modal", modal);
            linha.putAll(risco(ocorrencia, modal));
            riscos.add(linha);
        }
        corpo.put("riscoPorModal", riscos);
        corpo.put("proximosStatus", OcorrenciaService.proximosStatus(ocorrencia.getStatus()));
        corpo.put("acoes", Conversor.lista(acaoService.listarPorOcorrencia(id), Conversor::acao));
        return Resposta.ok(corpo);
    }

    private Resposta registrarOcorrencia(Requisicao requisicao) {
        Ocorrencia criada = ocorrenciaService.registrar(lerOcorrencia(requisicao.corpo()), requisicao.idUsuario());
        return Resposta.criado(Conversor.ocorrencia(criada));
    }

    private Resposta atualizar(Requisicao requisicao) {
        Ocorrencia atualizada = ocorrenciaService.atualizar(requisicao.id("id"), lerOcorrencia(requisicao.corpo()),
                requisicao.idUsuario());
        return Resposta.ok(Conversor.ocorrencia(atualizada));
    }

    private Resposta excluir(Requisicao requisicao) {
        ocorrenciaService.excluir(requisicao.id("id"), requisicao.idUsuario());
        return Resposta.semConteudo();
    }

    /** PATCH com {"status": "CONFIRMADA", "descricao": "Equipe da CET no local"}. */
    private Resposta alterarStatus(Requisicao requisicao) {
        CorpoJson corpo = requisicao.corpo();
        Ocorrencia ocorrencia = ocorrenciaService.alterarStatus(requisicao.id("id"),
                corpo.opcao("status", StatusOcorrencia.class), corpo.texto("descricao"), requisicao.idUsuario());
        return Resposta.ok(Conversor.ocorrencia(ocorrencia));
    }

    private Resposta listarAcoes(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(acaoService.listarPorOcorrencia(requisicao.id("id")), Conversor::acao));
    }

    private Resposta registrarAcao(Requisicao requisicao) {
        return Resposta.criado(Conversor.acao(acaoService.registrar(requisicao.id("id"),
                requisicao.corpo().texto("descricao"), requisicao.idUsuario())));
    }

    private Resposta atualizarAcao(Requisicao requisicao) {
        return Resposta.ok(Conversor.acao(acaoService.atualizar(requisicao.id("id"),
                requisicao.corpo().texto("descricao"), requisicao.idUsuario())));
    }

    private Resposta excluirAcao(Requisicao requisicao) {
        acaoService.excluir(requisicao.id("id"), requisicao.idUsuario());
        return Resposta.semConteudo();
    }

    private Map<String, Object> risco(Ocorrencia ocorrencia, Modal modal) {
        double valor = riscoService.calcular(ocorrencia, modal);
        return Conversor.risco(valor, riscoService.classificar(valor), riscoService.deveAlertar(valor));
    }

    /** Monta a ocorrência a partir do JSON; a validação de conteúdo fica na OcorrenciaService. */
    private Ocorrencia lerOcorrencia(CorpoJson corpo) {
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setTitulo(corpo.texto("titulo"));
        ocorrencia.setDescricao(corpo.texto("descricao"));
        ocorrencia.setTipo(corpo.opcao("tipo", TipoOcorrencia.class));
        Integer gravidade = corpo.inteiro("gravidade");
        ocorrencia.setGravidade(gravidade == null ? 0 : gravidade);

        List<OcorrenciaVia> vias = new ArrayList<>();
        for (CorpoJson item : corpo.lista("vias")) {
            Long idVia = item.longo("idVia");
            Via via = idVia == null ? null : new Via(idVia, null, null);
            vias.add(new OcorrenciaVia(via, item.opcao("nivelImpacto", NivelImpacto.class)));
        }
        ocorrencia.setVias(vias);
        return ocorrencia;
    }
}
