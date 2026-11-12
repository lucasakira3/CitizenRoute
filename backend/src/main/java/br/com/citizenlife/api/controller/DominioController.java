package br.com.citizenlife.api.controller;

import br.com.citizenlife.api.Requisicao;
import br.com.citizenlife.api.Resposta;
import br.com.citizenlife.api.Roteador;
import br.com.citizenlife.model.Cidadao;
import br.com.citizenlife.model.CriterioRota;
import br.com.citizenlife.model.Modal;
import br.com.citizenlife.model.NivelImpacto;
import br.com.citizenlife.model.NivelRisco;
import br.com.citizenlife.model.StatusOcorrencia;
import br.com.citizenlife.model.TipoEndereco;
import br.com.citizenlife.model.TipoOcorrencia;
import br.com.citizenlife.service.EnderecoService;
import br.com.citizenlife.service.RiscoService;
import br.com.citizenlife.service.RotaService;

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
            Map<String, Object> item = opcao(modal, modal.getRotulo());
            item.put("velocidadeMediaKmh", modal.getVelocidadeMediaKmh());
            item.put("motorizado", modal.isMotorizado());
            modais.add(item);
        }
        corpo.put("modais", modais);

        List<Map<String, Object>> tipos = new ArrayList<>();
        for (TipoOcorrencia tipo : TipoOcorrencia.values()) {
            Map<String, Object> item = opcao(tipo, tipo.getRotulo());
            item.put("peso", tipo.getPeso());
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

        List<Map<String, Object>> criterios = new ArrayList<>();
        for (CriterioRota criterio : CriterioRota.values()) {
            Map<String, Object> item = opcao(criterio, criterio.getRotulo());
            item.put("descricao", criterio.getDescricao());
            criterios.add(item);
        }
        corpo.put("criteriosRota", criterios);

        List<Map<String, Object>> tiposEndereco = new ArrayList<>();
        for (TipoEndereco tipo : TipoEndereco.values()) {
            Map<String, Object> item = opcao(tipo, tipo.getRotulo());
            item.put("unico", tipo.isUnico());
            tiposEndereco.add(item);
        }
        corpo.put("tiposEndereco", tiposEndereco);

        Map<String, Object> limites = new LinkedHashMap<>();
        limites.put("moderado", RiscoService.LIMITE_MODERADO);
        limites.put("alto", RiscoService.LIMITE_ALTO);
        limites.put("critico", RiscoService.LIMITE_CRITICO);
        limites.put("alerta", RiscoService.LIMITE_ALERTA);
        corpo.put("limitesRisco", limites);
        corpo.put("maximoRotas", RotaService.MAXIMO_ROTAS);
        corpo.put("maximoEnderecos", EnderecoService.MAXIMO_ENDERECOS);
        corpo.put("modalPadraoRisco", Cidadao.MODAL_PADRAO_RISCO);

        return Resposta.ok(corpo);
    }

    private Map<String, Object> opcao(Enum<?> valor, String rotulo) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("codigo", valor.name());
        item.put("rotulo", rotulo);
        return item;
    }
}
