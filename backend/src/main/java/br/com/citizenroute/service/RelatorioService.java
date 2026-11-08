package br.com.citizenroute.service;

import br.com.citizenroute.dao.FiltroOcorrencia;
import br.com.citizenroute.dao.OcorrenciaDao;
import br.com.citizenroute.dao.RelatorioDao;
import br.com.citizenroute.model.Modal;
import br.com.citizenroute.model.Ocorrencia;
import br.com.citizenroute.model.relatorio.ExposicaoModal;
import br.com.citizenroute.model.relatorio.OcorrenciasPorRegiao;
import br.com.citizenroute.model.relatorio.ResumoPainel;
import br.com.citizenroute.model.relatorio.TempoRespostaOrgao;
import br.com.citizenroute.model.relatorio.ViaCritica;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Relatórios gerenciais. A maior parte vem pronta do SQL; a exposição por modal usa também a matriz de risco. */
public class RelatorioService {

    private final RelatorioDao relatorioDao;
    private final OcorrenciaDao ocorrenciaDao;
    private final RiscoService riscoService;

    public RelatorioService(RelatorioDao relatorioDao, OcorrenciaDao ocorrenciaDao, RiscoService riscoService) {
        this.relatorioDao = relatorioDao;
        this.ocorrenciaDao = ocorrenciaDao;
        this.riscoService = riscoService;
    }

    public ResumoPainel resumo() {
        return relatorioDao.resumo();
    }

    public List<ViaCritica> viasCriticas() {
        return relatorioDao.viasCriticas();
    }

    public List<OcorrenciasPorRegiao> ocorrenciasPorRegiao() {
        return relatorioDao.ocorrenciasPorRegiao();
    }

    public List<TempoRespostaOrgao> tempoRespostaPorOrgao() {
        return relatorioDao.tempoRespostaPorOrgao();
    }

    /**
     * Junta as contagens do SQL com o risco médio que as ocorrências ativas de hoje
     * representam para cada modal. Todos os modais aparecem, mesmo sem cidadãos cadastrados.
     */
    public List<ExposicaoModal> exposicaoPorModal() {
        Map<Modal, ExposicaoModal> porModal = new EnumMap<>(Modal.class);
        for (ExposicaoModal linha : relatorioDao.exposicaoPorModal()) {
            porModal.put(linha.modal(), linha);
        }
        List<Ocorrencia> ativas = ocorrenciaDao.listar(FiltroOcorrencia.ativas());

        List<ExposicaoModal> resultado = new ArrayList<>();
        for (Modal modal : Modal.values()) {
            ExposicaoModal linha = porModal.getOrDefault(modal, new ExposicaoModal(modal, 0, 0, 0, 0));
            double riscoMedio = ativas.stream()
                    .mapToDouble(ocorrencia -> riscoService.calcular(ocorrencia, modal))
                    .average()
                    .orElse(0);
            resultado.add(linha.comRiscoMedio(RiscoService.arredondar(riscoMedio)));
        }
        resultado.sort(Comparator.comparingDouble(ExposicaoModal::riscoMedioAtual).reversed());
        return resultado;
    }
}
