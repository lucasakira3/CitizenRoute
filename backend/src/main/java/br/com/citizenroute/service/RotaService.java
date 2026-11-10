package br.com.citizenroute.service;

import br.com.citizenroute.dao.RotaDao;
import br.com.citizenroute.exception.AcessoNegadoException;
import br.com.citizenroute.exception.RecursoNaoEncontradoException;
import br.com.citizenroute.exception.RegraNegocioException;
import br.com.citizenroute.exception.ValidacaoException;
import br.com.citizenroute.model.Cidadao;
import br.com.citizenroute.model.PontoRota;
import br.com.citizenroute.model.Rota;
import br.com.citizenroute.model.Via;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rotas salvas pelo cidadão (saída → destino). Cada cidadão só vê e altera as próprias rotas;
 * gestores podem consultar as de qualquer cidadão.
 *
 * <p>O caminho é calculado no front-end, sobre o mapa do OpenStreetMap. O Java recebe o resultado
 * (pontos, distância e as vias cadastradas que o caminho percorre), valida e grava.</p>
 */
public class RotaService {

    public static final int MAXIMO_ROTAS = 10;
    public static final int MAXIMO_VIAS_POR_ROTA = 60;
    public static final int DISTANCIA_MAXIMA_METROS = 150_000;
    /** O plano do mapa tem cerca de 2.400 × 3.400 unidades; a folga cobre os municípios vizinhos. */
    private static final double LIMITE_PLANO = 5_000;

    private final RotaDao rotaDao;
    private final ViaService viaService;
    private final CidadaoService cidadaoService;
    private final AutenticacaoService autenticacao;

    public RotaService(RotaDao rotaDao, ViaService viaService, CidadaoService cidadaoService,
                       AutenticacaoService autenticacao) {
        this.rotaDao = rotaDao;
        this.viaService = viaService;
        this.cidadaoService = cidadaoService;
        this.autenticacao = autenticacao;
    }

    public List<Rota> listar(long idCidadao, Long idAtor) {
        autenticacao.exigirProprioOuGestor(idAtor, idCidadao, "ver as rotas");
        cidadaoService.buscar(idCidadao);
        return rotaDao.listarPorCidadao(idCidadao);
    }

    /** Rotas de um cidadão sem conferir permissão: uso interno das outras services. */
    List<Rota> rotasDo(long idCidadao) {
        return rotaDao.listarPorCidadao(idCidadao);
    }

    public Rota buscar(long idCidadao, long idRota, Long idAtor) {
        autenticacao.exigirProprioOuGestor(idAtor, idCidadao, "ver a rota");
        return rotaDoCidadao(idCidadao, idRota);
    }

    public Rota criar(long idCidadao, Rota dados, List<Long> idsVias, Long idAtor) {
        Cidadao cidadao = exigirDono(idCidadao, idAtor);
        Rota rota = validar(dados, idsVias);
        if (rotaDao.listarPorCidadao(idCidadao).size() >= MAXIMO_ROTAS) {
            throw new RegraNegocioException("Você já tem " + MAXIMO_ROTAS + " rotas salvas. Exclua uma antes de criar outra.");
        }
        rota.setIdCidadao(cidadao.getId());
        rota.setDataCriacao(LocalDateTime.now());
        return rotaDao.inserir(rota);
    }

    public Rota atualizar(long idCidadao, long idRota, Rota dados, List<Long> idsVias, Long idAtor) {
        exigirDono(idCidadao, idAtor);
        Rota atual = rotaDoCidadao(idCidadao, idRota);
        Rota rota = validar(dados, idsVias);
        rota.setId(atual.getId());
        rota.setIdCidadao(atual.getIdCidadao());
        rota.setDataCriacao(atual.getDataCriacao());
        if (!rotaDao.atualizar(rota)) {
            throw new RecursoNaoEncontradoException("Rota " + idRota + " não encontrada.");
        }
        return rota;
    }

    public void excluir(long idCidadao, long idRota, Long idAtor) {
        exigirDono(idCidadao, idAtor);
        rotaDoCidadao(idCidadao, idRota);
        if (!rotaDao.excluir(idRota)) {
            throw new RecursoNaoEncontradoException("Rota " + idRota + " não encontrada.");
        }
    }

    /** Converte a lista de ids na lista de vias do cadastro, na ordem, sem repetir e conferindo se existem. */
    public List<Via> viasDoCaminho(List<Long> idsVias) {
        Set<Long> unicos = new LinkedHashSet<>(idsVias == null ? List.of() : idsVias);
        unicos.remove(null);
        if (unicos.size() > MAXIMO_VIAS_POR_ROTA) {
            throw new ValidacaoException("A rota passa por vias demais.",
                    Map.of("vias", "Uma rota pode ter no máximo " + MAXIMO_VIAS_POR_ROTA + " vias cadastradas."));
        }
        List<Via> vias = new ArrayList<>();
        for (Long idVia : unicos) {
            try {
                vias.add(viaService.buscar(idVia));
            } catch (RecursoNaoEncontradoException e) {
                throw new ValidacaoException("Uma das vias da rota não existe mais. Calcule a rota de novo.",
                        Map.of("vias", "A via " + idVia + " não existe mais."));
            }
        }
        return vias;
    }

    private Rota validar(Rota dados, List<Long> idsVias) {
        Validador validador = new Validador();
        String nome = validador.texto("nome", dados.getNome(), "Nome da rota", 2, 60);
        PontoRota origem = validarPonto(validador, "origem", "a saída", "da saída", dados.getOrigem());
        PontoRota destino = validarPonto(validador, "destino", "o destino", "do destino", dados.getDestino());
        validador.obrigatorio("criterio", dados.getCriterio(), "Escolha se a rota é a mais rápida ou a mais segura.");
        validador.exigir(dados.getDistanciaMetros() > 0 && dados.getDistanciaMetros() <= DISTANCIA_MAXIMA_METROS,
                "distanciaMetros", "Distância inválida. Calcule a rota de novo.");
        if (origem != null && destino != null) {
            validador.exigir(Math.hypot(origem.x() - destino.x(), origem.y() - destino.y()) >= 1,
                    "destino", "A saída e o destino são o mesmo lugar.");
        }
        validador.validar();

        Rota rota = new Rota(null, null, nome, origem, destino, dados.getCriterio(), dados.getDistanciaMetros());
        rota.setVias(viasDoCaminho(idsVias));
        return rota;
    }

    /** rotulo: "a saída" ou "o destino"; deRotulo: "da saída" ou "do destino". */
    private PontoRota validarPonto(Validador validador, String campo, String rotulo, String deRotulo,
                                   PontoRota ponto) {
        if (ponto == null) {
            validador.erro(campo, "Informe " + rotulo + ".");
            return null;
        }
        String descricao = validador.texto(campo, ponto.descricao(), "Nome " + deRotulo, 2, 120);
        boolean dentro = ponto.x() >= 0 && ponto.y() >= 0 && ponto.x() <= LIMITE_PLANO && ponto.y() <= LIMITE_PLANO;
        validador.exigir(dentro, campo, "O ponto escolhido para " + rotulo + " está fora do mapa.");
        return new PontoRota(descricao, ponto.x(), ponto.y());
    }

    private Rota rotaDoCidadao(long idCidadao, long idRota) {
        Rota rota = rotaDao.buscarPorId(idRota)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Rota " + idRota + " não encontrada."));
        if (rota.getIdCidadao() != idCidadao) {
            throw new RecursoNaoEncontradoException("Rota " + idRota + " não encontrada.");
        }
        return rota;
    }

    private Cidadao exigirDono(long idCidadao, Long idAtor) {
        Cidadao cidadao = autenticacao.exigirCidadao(idAtor, "salvar rotas");
        if (cidadao.getId() != idCidadao) {
            throw new AcessoNegadoException("Você só pode alterar as suas próprias rotas.");
        }
        return cidadao;
    }
}
