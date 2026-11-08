package br.com.citizenroute.service;

import br.com.citizenroute.dao.FiltroOcorrencia;
import br.com.citizenroute.dao.OcorrenciaDao;
import br.com.citizenroute.dao.ViaMonitoradaDao;
import br.com.citizenroute.model.Alerta;
import br.com.citizenroute.model.Cidadao;
import br.com.citizenroute.model.Ocorrencia;
import br.com.citizenroute.model.ViaMonitorada;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Cruza as ocorrências ativas com as vias que o cidadão monitora e calcula o risco para o modal dele.
 * É o "o cidadão se protege" do projeto.
 */
public class AlertaService {

    private final OcorrenciaDao ocorrenciaDao;
    private final ViaMonitoradaDao viaMonitoradaDao;
    private final CidadaoService cidadaoService;
    private final RiscoService riscoService;
    private final AutenticacaoService autenticacao;

    public AlertaService(OcorrenciaDao ocorrenciaDao, ViaMonitoradaDao viaMonitoradaDao,
                         CidadaoService cidadaoService, RiscoService riscoService, AutenticacaoService autenticacao) {
        this.ocorrenciaDao = ocorrenciaDao;
        this.viaMonitoradaDao = viaMonitoradaDao;
        this.cidadaoService = cidadaoService;
        this.riscoService = riscoService;
        this.autenticacao = autenticacao;
    }

    /**
     * Devolve todas as ocorrências ativas nas vias monitoradas, das mais perigosas para as menos.
     * As que passam do limite de risco vêm marcadas como alerta.
     */
    public List<Alerta> alertasDoCidadao(long idCidadao, Long idAtor) {
        autenticacao.exigirProprioOuGestor(idAtor, idCidadao, "ver os alertas");
        Cidadao cidadao = cidadaoService.buscar(idCidadao);
        List<ViaMonitorada> monitoradas = viaMonitoradaDao.listarPorCidadao(idCidadao);
        List<Ocorrencia> ocorrencias = ocorrenciaDao.listar(FiltroOcorrencia.ativasNasViasDo(idCidadao));

        List<Alerta> alertas = new ArrayList<>();
        for (Ocorrencia ocorrencia : ocorrencias) {
            List<ViaMonitorada> afetadas = monitoradas.stream()
                    .filter(monitorada -> ocorrencia.afetaVia(monitorada.getVia().getId()))
                    .toList();
            double risco = riscoService.calcular(ocorrencia, cidadao.getModal());
            alertas.add(new Alerta(ocorrencia, afetadas, risco, riscoService.classificar(risco),
                    riscoService.deveAlertar(risco)));
        }
        alertas.sort(Comparator.comparing(Alerta::isAcimaDoLimite).reversed()
                .thenComparing(Comparator.comparingDouble(Alerta::getRisco).reversed()));
        return alertas;
    }
}
