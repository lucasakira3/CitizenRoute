package br.com.citizenlife.api.controller;

import br.com.citizenlife.api.Conversor;
import br.com.citizenlife.api.Requisicao;
import br.com.citizenlife.api.Resposta;
import br.com.citizenlife.api.Roteador;
import br.com.citizenlife.model.Modal;
import br.com.citizenlife.model.Ocorrencia;
import br.com.citizenlife.service.OcorrenciaService;
import br.com.citizenlife.service.RiscoService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ocorrências do mapa de riscos (somente leitura).
 * <ul>
 *   <li>GET /api/ocorrencias?ativas=true&amp;modal=MOTO: lista, com o risco para o modal informado</li>
 *   <li>GET /api/ocorrencias/{id}: detalhe com o risco para cada modal</li>
 * </ul>
 */
public class OcorrenciaController {

    private final OcorrenciaService ocorrenciaService;
    private final RiscoService riscoService;

    public OcorrenciaController(OcorrenciaService ocorrenciaService, RiscoService riscoService) {
        this.ocorrenciaService = ocorrenciaService;
        this.riscoService = riscoService;
    }

    public void registrar(Roteador roteador) {
        roteador.get("/api/ocorrencias", this::listar)
                .get("/api/ocorrencias/{id}", this::detalhar);
    }

    /** Com ?modal=MOTO, cada item vem com o risco calculado para quem anda de moto. */
    private Resposta listar(Requisicao requisicao) {
        List<Ocorrencia> ocorrencias = ocorrenciaService.listar(requisicao.parametroBooleano("ativas"));
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

    /** Detalhe com o risco da mesma ocorrência para cada modal. */
    private Resposta detalhar(Requisicao requisicao) {
        Ocorrencia ocorrencia = ocorrenciaService.buscar(requisicao.id("id"));
        Map<String, Object> corpo = Conversor.ocorrencia(ocorrencia);

        List<Map<String, Object>> riscos = new ArrayList<>();
        for (Modal modal : Modal.values()) {
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("modal", modal);
            linha.putAll(risco(ocorrencia, modal));
            riscos.add(linha);
        }
        corpo.put("riscoPorModal", riscos);
        return Resposta.ok(corpo);
    }

    private Map<String, Object> risco(Ocorrencia ocorrencia, Modal modal) {
        double valor = riscoService.calcular(ocorrencia, modal);
        return Conversor.risco(valor, riscoService.classificar(valor), riscoService.deveAlertar(valor));
    }
}
