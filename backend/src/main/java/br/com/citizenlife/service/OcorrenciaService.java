package br.com.citizenlife.service;

import br.com.citizenlife.dao.OcorrenciaDao;
import br.com.citizenlife.dao.RotaDao;
import br.com.citizenlife.exception.ValidacaoException;
import br.com.citizenlife.model.Cidadao;
import br.com.citizenlife.model.Ocorrencia;
import br.com.citizenlife.model.Rota;

import java.util.Comparator;
import java.util.List;

/** Ocorrências do mapa de riscos e os alertas: as que estão no caminho de alguém. */
public class OcorrenciaService {

    private final OcorrenciaDao ocorrenciaDao;
    private final RotaDao rotaDao;

    public OcorrenciaService(OcorrenciaDao ocorrenciaDao, RotaDao rotaDao) {
        this.ocorrenciaDao = ocorrenciaDao;
        this.rotaDao = rotaDao;
    }

    /** Todas as ocorrências, das mais perigosas para as menos, para o modal informado. */
    public List<Ocorrencia> listar(String modal) {
        String modalValido = validarModal(modal);
        return ocorrenciaDao.listar().stream()
                .sorted(Comparator.comparingDouble((Ocorrencia o) -> o.calcularRisco(modalValido)).reversed())
                .toList();
    }

    /** Ocorrências nas vias de um caminho recém-calculado (a lista de ids das vias). */
    public List<Ocorrencia> noCaminho(List<Long> idsVias, String modal) {
        List<Long> ids = idsVias == null ? List.of() : idsVias;
        return listar(modal).stream().filter(o -> ids.contains(o.getVia().getId())).toList();
    }

    /** Alertas do cidadão: ocorrências em alguma via das rotas salvas, calculadas para o modal dele. */
    public List<Ocorrencia> alertas(Cidadao cidadao) {
        List<Rota> rotas = rotaDao.listarPorCidadao(cidadao.getId());
        return listar(cidadao.modalParaRisco()).stream()
                .filter(o -> rotas.stream().anyMatch(rota -> rota.passaPor(o.getVia().getId())))
                .toList();
    }

    private String validarModal(String modal) {
        if (!Ocorrencia.MODAIS.contains(modal)) {
            throw new ValidacaoException("modal", "Escolha o meio de transporte.");
        }
        return modal;
    }
}
