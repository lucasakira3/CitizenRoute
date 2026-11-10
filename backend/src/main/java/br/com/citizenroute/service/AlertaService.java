package br.com.citizenroute.service;

import br.com.citizenroute.dao.FiltroOcorrencia;
import br.com.citizenroute.dao.OcorrenciaDao;
import br.com.citizenroute.exception.ValidacaoException;
import br.com.citizenroute.model.Alerta;
import br.com.citizenroute.model.Cidadao;
import br.com.citizenroute.model.Modal;
import br.com.citizenroute.model.Ocorrencia;
import br.com.citizenroute.model.Rota;
import br.com.citizenroute.model.Via;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Cruza as ocorrências ativas com um caminho e calcula o risco para o modal de quem vai passar.
 * É o "o cidadão se protege" do projeto. Serve para duas coisas:
 * <ul>
 *   <li>os alertas das rotas salvas do cidadão;</li>
 *   <li>a avaliação de uma rota recém-calculada no mapa, antes de salvar (ou sem login).</li>
 * </ul>
 */
public class AlertaService {

    private final OcorrenciaDao ocorrenciaDao;
    private final RotaService rotaService;
    private final CidadaoService cidadaoService;
    private final RiscoService riscoService;
    private final AutenticacaoService autenticacao;

    public AlertaService(OcorrenciaDao ocorrenciaDao, RotaService rotaService,
                         CidadaoService cidadaoService, RiscoService riscoService, AutenticacaoService autenticacao) {
        this.ocorrenciaDao = ocorrenciaDao;
        this.rotaService = rotaService;
        this.cidadaoService = cidadaoService;
        this.riscoService = riscoService;
        this.autenticacao = autenticacao;
    }

    /**
     * Ocorrências ativas nas vias das rotas salvas, das mais perigosas para as menos.
     * As que passam do limite de risco vêm marcadas como alerta.
     */
    public List<Alerta> alertasDoCidadao(long idCidadao, Long idAtor) {
        autenticacao.exigirProprioOuGestor(idAtor, idCidadao, "ver os alertas");
        Cidadao cidadao = cidadaoService.buscar(idCidadao);
        List<Rota> rotas = rotaService.rotasDo(idCidadao);
        List<Ocorrencia> ocorrencias = ocorrenciaDao.listar(FiltroOcorrencia.ativasNasRotasDo(idCidadao));

        List<Alerta> alertas = new ArrayList<>();
        for (Ocorrencia ocorrencia : ocorrencias) {
            List<Via> vias = new ArrayList<>();
            List<Rota> afetadas = new ArrayList<>();
            for (Rota rota : rotas) {
                List<Via> atingidas = viasAtingidas(ocorrencia, rota.getVias());
                if (!atingidas.isEmpty()) {
                    afetadas.add(rota);
                    atingidas.stream()
                            .filter(via -> vias.stream().noneMatch(outra -> outra.getId().equals(via.getId())))
                            .forEach(vias::add);
                }
            }
            alertas.add(criar(ocorrencia, vias, afetadas, cidadao.getModal()));
        }
        return ordenar(alertas);
    }

    /** Alertas de uma rota salva, para o modal do dono. */
    public List<Alerta> alertasDaRota(Rota rota) {
        Cidadao cidadao = cidadaoService.buscar(rota.getIdCidadao());
        return avaliar(rota.getVias(), cidadao.getModal(), List.of(rota));
    }

    /** Avalia um caminho recém-calculado: as vias cadastradas que ele percorre e o modal de quem vai passar. */
    public List<Alerta> avaliarCaminho(List<Long> idsVias, Modal modal) {
        if (modal == null) {
            throw new ValidacaoException("Escolha o meio de transporte.", Map.of("modal", "Escolha o meio de transporte."));
        }
        return avaliar(rotaService.viasDoCaminho(idsVias), modal, List.of());
    }

    private List<Alerta> avaliar(List<Via> caminho, Modal modal, List<Rota> rotas) {
        if (caminho.isEmpty()) {
            return List.of();
        }
        List<Alerta> alertas = new ArrayList<>();
        for (Ocorrencia ocorrencia : ocorrenciaDao.listar(FiltroOcorrencia.ativas())) {
            List<Via> atingidas = viasAtingidas(ocorrencia, caminho);
            if (!atingidas.isEmpty()) {
                alertas.add(criar(ocorrencia, atingidas, rotas, modal));
            }
        }
        return ordenar(alertas);
    }

    private List<Via> viasAtingidas(Ocorrencia ocorrencia, List<Via> caminho) {
        return caminho.stream().filter(via -> ocorrencia.afetaVia(via.getId())).toList();
    }

    private Alerta criar(Ocorrencia ocorrencia, List<Via> vias, List<Rota> rotas, Modal modal) {
        double risco = riscoService.calcular(ocorrencia, modal);
        return new Alerta(ocorrencia, vias, rotas, risco, riscoService.classificar(risco),
                riscoService.deveAlertar(risco));
    }

    private List<Alerta> ordenar(List<Alerta> alertas) {
        alertas.sort(Comparator.comparing(Alerta::isAcimaDoLimite).reversed()
                .thenComparing(Comparator.comparingDouble(Alerta::getRisco).reversed()));
        return alertas;
    }
}
