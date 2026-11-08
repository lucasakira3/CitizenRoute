package br.com.citizenroute.service;

import br.com.citizenroute.dao.ViaMonitoradaDao;
import br.com.citizenroute.exception.AcessoNegadoException;
import br.com.citizenroute.exception.RecursoNaoEncontradoException;
import br.com.citizenroute.exception.RegraNegocioException;
import br.com.citizenroute.model.Cidadao;
import br.com.citizenroute.model.Via;
import br.com.citizenroute.model.ViaMonitorada;

import java.time.LocalDateTime;
import java.util.List;

/** Trajetos salvos pelo cidadão. Cada cidadão só altera a própria lista. */
public class ViaMonitoradaService {

    public static final int MAXIMO_VIAS = 15;

    private final ViaMonitoradaDao viaMonitoradaDao;
    private final ViaService viaService;
    private final CidadaoService cidadaoService;
    private final AutenticacaoService autenticacao;

    public ViaMonitoradaService(ViaMonitoradaDao viaMonitoradaDao, ViaService viaService,
                                CidadaoService cidadaoService, AutenticacaoService autenticacao) {
        this.viaMonitoradaDao = viaMonitoradaDao;
        this.viaService = viaService;
        this.cidadaoService = cidadaoService;
        this.autenticacao = autenticacao;
    }

    public List<ViaMonitorada> listar(long idCidadao, Long idAtor) {
        autenticacao.exigirProprioOuGestor(idAtor, idCidadao, "ver as vias monitoradas");
        cidadaoService.buscar(idCidadao);
        return viaMonitoradaDao.listarPorCidadao(idCidadao);
    }

    public ViaMonitorada adicionar(long idCidadao, Long idVia, String apelido, Long idAtor) {
        Cidadao cidadao = exigirDono(idCidadao, idAtor);
        Validador validador = new Validador();
        validador.obrigatorio("idVia", idVia, "Escolha a via que você quer monitorar.");
        String apelidoLimpo = validador.texto("apelido", apelido, "Apelido", 2, 60);
        validador.validar();

        Via via = viaService.buscar(idVia);
        if (viaMonitoradaDao.buscar(idCidadao, idVia).isPresent()) {
            throw new RegraNegocioException("A via " + via.getNome() + " já está na sua lista.");
        }
        if (viaMonitoradaDao.listarPorCidadao(idCidadao).size() >= MAXIMO_VIAS) {
            throw new RegraNegocioException("Você já monitora " + MAXIMO_VIAS + " vias. Remova uma antes de adicionar outra.");
        }

        ViaMonitorada nova = new ViaMonitorada(cidadao.getId(), via, apelidoLimpo);
        nova.setDataInclusao(LocalDateTime.now());
        return viaMonitoradaDao.inserir(nova);
    }

    public ViaMonitorada atualizar(long idCidadao, long idVia, String apelido, Long idAtor) {
        exigirDono(idCidadao, idAtor);
        Validador validador = new Validador();
        String apelidoLimpo = validador.texto("apelido", apelido, "Apelido", 2, 60);
        validador.validar();

        ViaMonitorada atual = viaMonitoradaDao.buscar(idCidadao, idVia)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Esta via não está na sua lista."));
        atual.setApelido(apelidoLimpo);
        viaMonitoradaDao.atualizar(atual);
        return atual;
    }

    public void remover(long idCidadao, long idVia, Long idAtor) {
        exigirDono(idCidadao, idAtor);
        if (!viaMonitoradaDao.excluir(idCidadao, idVia)) {
            throw new RecursoNaoEncontradoException("Esta via não está na sua lista.");
        }
    }

    private Cidadao exigirDono(long idCidadao, Long idAtor) {
        Cidadao cidadao = autenticacao.exigirCidadao(idAtor, "monitorar vias");
        if (cidadao.getId() != idCidadao) {
            throw new AcessoNegadoException("Você só pode alterar as suas próprias vias monitoradas.");
        }
        return cidadao;
    }
}
