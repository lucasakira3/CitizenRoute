package br.com.citizenlife.service;

import br.com.citizenlife.dao.RotaDao;
import br.com.citizenlife.exception.CitizenLifeException;
import br.com.citizenlife.exception.ValidacaoException;
import br.com.citizenlife.model.Rota;
import br.com.citizenlife.model.Via;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rotas salvas pelo cidadão. O caminho é calculado no site, sobre o mapa;
 * o Java recebe o resultado (saída, destino, distância e as vias percorridas), valida e grava.
 */
public class RotaService {

    public static final int MAXIMO_ROTAS = 10;

    private final RotaDao rotaDao;
    private final CidadaoService cidadaoService;

    public RotaService(RotaDao rotaDao, CidadaoService cidadaoService) {
        this.rotaDao = rotaDao;
        this.cidadaoService = cidadaoService;
    }

    public List<Rota> listar(long idCidadao, Long idAtor) {
        cidadaoService.exigirDono(idAtor, idCidadao);
        return rotaDao.listarPorCidadao(idCidadao);
    }

    public Rota buscar(long idCidadao, long idRota, Long idAtor) {
        cidadaoService.exigirDono(idAtor, idCidadao);
        return rotaDoCidadao(idCidadao, idRota);
    }

    public Rota criar(long idCidadao, Rota dados, List<Long> idsVias, Long idAtor) {
        cidadaoService.exigirDono(idAtor, idCidadao);
        validar(dados, idsVias);
        if (rotaDao.listarPorCidadao(idCidadao).size() >= MAXIMO_ROTAS) {
            throw new ValidacaoException("nome", "Você já tem " + MAXIMO_ROTAS + " rotas. Exclua uma antes de criar outra.");
        }
        dados.setIdCidadao(idCidadao);
        dados.setDataCriacao(LocalDateTime.now());
        return rotaDao.inserir(dados);
    }

    public Rota atualizar(long idCidadao, long idRota, Rota dados, List<Long> idsVias, Long idAtor) {
        cidadaoService.exigirDono(idAtor, idCidadao);
        Rota atual = rotaDoCidadao(idCidadao, idRota);
        validar(dados, idsVias);
        dados.setId(idRota);
        dados.setIdCidadao(idCidadao);
        dados.setDataCriacao(atual.getDataCriacao());
        rotaDao.atualizar(dados);
        return dados;
    }

    public void excluir(long idCidadao, long idRota, Long idAtor) {
        cidadaoService.exigirDono(idAtor, idCidadao);
        rotaDoCidadao(idCidadao, idRota);
        rotaDao.excluir(idRota);
    }

    /** As vias cadastradas: o site usa para descobrir por quais delas o caminho passa. */
    public List<Via> listarVias() {
        return rotaDao.listarVias();
    }

    /** Confere os campos e troca os ids das vias pelos objetos do cadastro, na ordem do caminho. */
    private void validar(Rota rota, List<Long> idsVias) {
        ValidacaoException erros = new ValidacaoException();
        rota.setNome(erros.texto("nome", rota.getNome(), "Nome da rota", 2, 60));
        rota.setOrigemDescricao(erros.texto("origem", rota.getOrigemDescricao(), "Saída", 2, 120));
        rota.setDestinoDescricao(erros.texto("destino", rota.getDestinoDescricao(), "Destino", 2, 120));
        erros.exigir(rota.getDistanciaMetros() > 0 && rota.getDistanciaMetros() <= 150_000,
                "distanciaMetros", "Distância inválida. Calcule a rota de novo.");

        Map<Long, Via> cadastro = rotaDao.listarVias().stream().collect(Collectors.toMap(Via::getId, Function.identity()));
        List<Via> vias = new ArrayList<>();
        for (Long id : idsVias == null ? List.<Long>of() : idsVias) {
            Via via = cadastro.get(id);
            erros.exigir(via != null, "vias", "Uma das vias da rota não existe mais. Calcule a rota de novo.");
            if (via != null && !vias.contains(via)) {
                vias.add(via);
            }
        }
        erros.lancarSeHouverErros();
        rota.setVias(vias);
    }

    private Rota rotaDoCidadao(long idCidadao, long idRota) {
        return rotaDao.buscarPorId(idRota)
                .filter(rota -> rota.getIdCidadao() == idCidadao)
                .orElseThrow(() -> CitizenLifeException.naoEncontrado("Rota não encontrada."));
    }
}
