package br.com.citizenroute.api.controller;

import br.com.citizenroute.api.Conversor;
import br.com.citizenroute.api.Requisicao;
import br.com.citizenroute.api.Resposta;
import br.com.citizenroute.api.Roteador;
import br.com.citizenroute.model.relatorio.ExposicaoModal;
import br.com.citizenroute.model.relatorio.OcorrenciasPorRegiao;
import br.com.citizenroute.model.relatorio.ResumoPainel;
import br.com.citizenroute.model.relatorio.TempoRespostaOrgao;
import br.com.citizenroute.model.relatorio.ViaCritica;
import br.com.citizenroute.service.RelatorioService;

import java.util.LinkedHashMap;
import java.util.Map;

/** Relatórios com SQL (JOIN e agregação): /api/relatorios/... */
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    public void registrar(Roteador roteador) {
        roteador.get("/api/relatorios/resumo", this::resumo)
                .get("/api/relatorios/vias-criticas", this::viasCriticas)
                .get("/api/relatorios/ocorrencias-por-regiao", this::porRegiao)
                .get("/api/relatorios/tempo-resposta", this::tempoResposta)
                .get("/api/relatorios/exposicao-modal", this::exposicaoModal);
    }

    private Resposta resumo(Requisicao requisicao) {
        ResumoPainel resumo = relatorioService.resumo();
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("ocorrenciasAtivas", resumo.ocorrenciasAtivas());
        mapa.put("aguardandoConfirmacao", resumo.aguardandoConfirmacao());
        mapa.put("emAtendimento", resumo.emAtendimento());
        mapa.put("gravesAtivas", resumo.gravesAtivas());
        mapa.put("resolvidasUltimos30Dias", resumo.resolvidasUltimos30Dias());
        mapa.put("tempoMedioRespostaMinutos", resumo.tempoMedioRespostaMinutos());
        mapa.put("cidadaos", resumo.cidadaos());
        mapa.put("viasMonitoradas", resumo.viasMonitoradas());
        return Resposta.ok(mapa);
    }

    private Resposta viasCriticas(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(relatorioService.viasCriticas(), this::viaCritica));
    }

    private Resposta porRegiao(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(relatorioService.ocorrenciasPorRegiao(), this::ocorrenciasPorRegiao));
    }

    private Resposta tempoResposta(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(relatorioService.tempoRespostaPorOrgao(), this::tempoRespostaOrgao));
    }

    private Resposta exposicaoModal(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(relatorioService.exposicaoPorModal(), this::exposicao));
    }

    private Map<String, Object> viaCritica(ViaCritica linha) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("idVia", linha.idVia());
        mapa.put("via", linha.via());
        mapa.put("regiao", linha.regiao());
        mapa.put("totalOcorrencias", linha.totalOcorrencias());
        mapa.put("ocorrenciasAtivas", linha.ocorrenciasAtivas());
        mapa.put("gravidadeMedia", linha.gravidadeMedia());
        mapa.put("gravidadeMaxima", linha.gravidadeMaxima());
        return mapa;
    }

    private Map<String, Object> ocorrenciasPorRegiao(OcorrenciasPorRegiao linha) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("regiao", linha.regiao());
        mapa.put("tipo", linha.tipo());
        mapa.put("total", linha.total());
        mapa.put("ativas", linha.ativas());
        return mapa;
    }

    private Map<String, Object> tempoRespostaOrgao(TempoRespostaOrgao linha) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("orgao", linha.orgao());
        mapa.put("ocorrenciasAtendidas", linha.ocorrenciasAtendidas());
        mapa.put("minutosMedios", linha.minutosMedios());
        mapa.put("resolvidas", linha.resolvidas());
        return mapa;
    }

    private Map<String, Object> exposicao(ExposicaoModal linha) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("modal", linha.modal());
        mapa.put("cidadaos", linha.cidadaos());
        mapa.put("viasMonitoradas", linha.viasMonitoradas());
        mapa.put("ocorrenciasAtivasNasVias", linha.ocorrenciasAtivasNasVias());
        mapa.put("riscoMedioAtual", linha.riscoMedioAtual());
        return mapa;
    }
}
