package br.com.citizenroute.api.controller;

import br.com.citizenroute.api.Requisicao;
import br.com.citizenroute.api.Resposta;
import br.com.citizenroute.api.Roteador;
import br.com.citizenroute.model.Modal;
import br.com.citizenroute.model.NivelImpacto;
import br.com.citizenroute.model.NivelRisco;
import br.com.citizenroute.model.Orgao;
import br.com.citizenroute.model.StatusOcorrencia;
import br.com.citizenroute.model.TipoOcorrencia;
import br.com.citizenroute.service.OcorrenciaService;
import br.com.citizenroute.service.RiscoService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/dominio: opções e rótulos dos enums, a matriz de risco e os limites.
 * O front-end monta selects e legendas a partir daqui, então o Java é a única fonte dessas regras.
 */
public class DominioController {

    public void registrar(Roteador roteador) {
        roteador.get("/api/dominio", this::dominio);
    }

    private Resposta dominio(Requisicao requisicao) {
        Map<String, Object> corpo = new LinkedHashMap<>();

        List<Map<String, Object>> modais = new ArrayList<>();
        for (Modal modal : Modal.values()) {
            modais.add(opcao(modal, modal.getRotulo()));
        }
        corpo.put("modais", modais);

        List<Map<String, Object>> orgaos = new ArrayList<>();
        for (Orgao orgao : Orgao.values()) {
            Map<String, Object> item = opcao(orgao, orgao.getRotulo());
            item.put("atribuicao", orgao.getAtribuicao());
            orgaos.add(item);
        }
        corpo.put("orgaos", orgaos);

        List<Map<String, Object>> tipos = new ArrayList<>();
        for (TipoOcorrencia tipo : TipoOcorrencia.values()) {
            Map<String, Object> item = opcao(tipo, tipo.getRotulo());
            item.put("peso", tipo.getPeso());
            item.put("orgaoResponsavel", tipo.getOrgaoResponsavel());
            Map<String, Object> fatores = new LinkedHashMap<>();
            for (Modal modal : Modal.values()) {
                fatores.put(modal.name(), tipo.fatorExposicao(modal));
            }
            item.put("fatores", fatores);
            tipos.add(item);
        }
        corpo.put("tiposOcorrencia", tipos);

        List<Map<String, Object>> status = new ArrayList<>();
        for (StatusOcorrencia valor : StatusOcorrencia.values()) {
            Map<String, Object> item = opcao(valor, valor.getRotulo());
            item.put("ativa", valor.isAtiva());
            item.put("proximos", OcorrenciaService.proximosStatus(valor));
            status.add(item);
        }
        corpo.put("status", status);

        List<Map<String, Object>> impactos = new ArrayList<>();
        for (NivelImpacto nivel : NivelImpacto.values()) {
            impactos.add(opcao(nivel, nivel.getRotulo()));
        }
        corpo.put("niveisImpacto", impactos);

        List<Map<String, Object>> riscos = new ArrayList<>();
        for (NivelRisco nivel : NivelRisco.values()) {
            riscos.add(opcao(nivel, nivel.getRotulo()));
        }
        corpo.put("niveisRisco", riscos);

        Map<String, Object> limites = new LinkedHashMap<>();
        limites.put("moderado", RiscoService.LIMITE_MODERADO);
        limites.put("alto", RiscoService.LIMITE_ALTO);
        limites.put("critico", RiscoService.LIMITE_CRITICO);
        limites.put("alerta", RiscoService.LIMITE_ALERTA);
        corpo.put("limitesRisco", limites);

        return Resposta.ok(corpo);
    }

    private Map<String, Object> opcao(Enum<?> valor, String rotulo) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("codigo", valor.name());
        item.put("rotulo", rotulo);
        return item;
    }
}
