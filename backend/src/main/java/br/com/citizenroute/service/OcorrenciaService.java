package br.com.citizenroute.service;

import br.com.citizenroute.dao.FiltroOcorrencia;
import br.com.citizenroute.dao.OcorrenciaDao;
import br.com.citizenroute.dao.ViaDao;
import br.com.citizenroute.exception.AcessoNegadoException;
import br.com.citizenroute.exception.RecursoNaoEncontradoException;
import br.com.citizenroute.exception.RegraNegocioException;
import br.com.citizenroute.exception.ValidacaoException;
import br.com.citizenroute.model.Acao;
import br.com.citizenroute.model.Cidadao;
import br.com.citizenroute.model.Gestor;
import br.com.citizenroute.model.Ocorrencia;
import br.com.citizenroute.model.OcorrenciaVia;
import br.com.citizenroute.model.Orgao;
import br.com.citizenroute.model.StatusOcorrencia;
import br.com.citizenroute.model.TipoOcorrencia;
import br.com.citizenroute.model.Usuario;
import br.com.citizenroute.model.Via;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Regras do ciclo de vida das ocorrências.
 *
 * <ul>
 *   <li>Só cidadãos registram ocorrências; só gestores mudam o status.</li>
 *   <li>O autor pode editar ou excluir enquanto a ocorrência está ABERTA.</li>
 *   <li>O status segue um fluxo fixo: não existe volta de RESOLVIDA para ABERTA.</li>
 * </ul>
 */
public class OcorrenciaService {

    public static final int MAXIMO_VIAS = 10;

    /** Para cada status, os próximos status permitidos. */
    private static final Map<StatusOcorrencia, Set<StatusOcorrencia>> TRANSICOES = new EnumMap<>(StatusOcorrencia.class);

    static {
        TRANSICOES.put(StatusOcorrencia.ABERTA,
                EnumSet.of(StatusOcorrencia.CONFIRMADA, StatusOcorrencia.DESCARTADA));
        TRANSICOES.put(StatusOcorrencia.CONFIRMADA,
                EnumSet.of(StatusOcorrencia.EM_ATENDIMENTO, StatusOcorrencia.RESOLVIDA, StatusOcorrencia.DESCARTADA));
        TRANSICOES.put(StatusOcorrencia.EM_ATENDIMENTO,
                EnumSet.of(StatusOcorrencia.RESOLVIDA));
        TRANSICOES.put(StatusOcorrencia.RESOLVIDA, EnumSet.noneOf(StatusOcorrencia.class));
        TRANSICOES.put(StatusOcorrencia.DESCARTADA, EnumSet.noneOf(StatusOcorrencia.class));
    }

    private final OcorrenciaDao ocorrenciaDao;
    private final ViaDao viaDao;
    private final AutenticacaoService autenticacao;

    public OcorrenciaService(OcorrenciaDao ocorrenciaDao, ViaDao viaDao, AutenticacaoService autenticacao) {
        this.ocorrenciaDao = ocorrenciaDao;
        this.viaDao = viaDao;
        this.autenticacao = autenticacao;
    }

    /** Status para os quais a ocorrência pode ir a partir do status atual. */
    public static Set<StatusOcorrencia> proximosStatus(StatusOcorrencia atual) {
        return EnumSet.copyOf(TRANSICOES.getOrDefault(atual, EnumSet.noneOf(StatusOcorrencia.class)));
    }

    public static boolean podeMudar(StatusOcorrencia de, StatusOcorrencia para) {
        return TRANSICOES.getOrDefault(de, Set.of()).contains(para);
    }

    /** Tipos de ocorrência que chegam na fila de um órgão. */
    public static Set<TipoOcorrencia> tiposAtendidosPor(Orgao orgao) {
        Set<TipoOcorrencia> tipos = EnumSet.noneOf(TipoOcorrencia.class);
        for (TipoOcorrencia tipo : TipoOcorrencia.values()) {
            if (tipo.getOrgaoResponsavel() == orgao) {
                tipos.add(tipo);
            }
        }
        return tipos;
    }

    /**
     * Lista ocorrências com filtros opcionais (nulo significa "todos").
     * Quando o órgão é informado, só entram os tipos que esse órgão atende.
     */
    public List<Ocorrencia> listar(StatusOcorrencia status, TipoOcorrencia tipo, Orgao orgao, Long idRegiao,
                                   Long idVia, Long idAutor, boolean apenasAtivas) {
        Set<TipoOcorrencia> tipos = null;
        if (orgao != null) {
            tipos = tiposAtendidosPor(orgao);
            if (tipo != null) {
                tipos.retainAll(Set.of(tipo));
            }
            if (tipos.isEmpty()) {
                return List.of();
            }
        } else if (tipo != null) {
            tipos = EnumSet.of(tipo);
        }
        return ocorrenciaDao.listar(new FiltroOcorrencia(status, tipos, idRegiao, idVia, idAutor, null, apenasAtivas));
    }

    public Ocorrencia buscar(long id) {
        return ocorrenciaDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ocorrência " + id + " não encontrada."));
    }

    public Ocorrencia registrar(Ocorrencia dados, Long idAtor) {
        Cidadao autor = autenticacao.exigirCidadao(idAtor, "registrar ocorrências");
        Ocorrencia nova = validar(dados);
        nova.setAutor(autor);
        nova.setStatus(StatusOcorrencia.ABERTA);
        nova.setDataRegistro(LocalDateTime.now());
        return ocorrenciaDao.inserir(nova);
    }

    public Ocorrencia atualizar(long id, Ocorrencia dados, Long idAtor) {
        Cidadao ator = autenticacao.exigirCidadao(idAtor, "editar o relato de uma ocorrência");
        Ocorrencia atual = buscar(id);
        if (!atual.getAutor().getId().equals(ator.getId())) {
            throw new AcessoNegadoException("Só quem registrou a ocorrência pode editá-la.");
        }
        if (atual.getStatus() != StatusOcorrencia.ABERTA) {
            throw new RegraNegocioException("A ocorrência já foi analisada pela gestão e não pode mais ser editada.");
        }

        Ocorrencia validada = validar(dados);
        atual.setTitulo(validada.getTitulo());
        atual.setDescricao(validada.getDescricao());
        atual.setTipo(validada.getTipo());
        atual.setGravidade(validada.getGravidade());
        atual.setVias(validada.getVias());
        atual.setDataAtualizacao(LocalDateTime.now());
        if (!ocorrenciaDao.atualizar(atual)) {
            throw new RecursoNaoEncontradoException("Ocorrência " + id + " não encontrada.");
        }
        return atual;
    }

    /**
     * O autor exclui enquanto está ABERTA (ainda não foi analisada).
     * O gestor exclui ocorrências ABERTAS ou DESCARTADAS; as atendidas ficam no histórico.
     */
    public void excluir(long id, Long idAtor) {
        Usuario ator = autenticacao.exigirUsuario(idAtor);
        Ocorrencia ocorrencia = buscar(id);
        StatusOcorrencia status = ocorrencia.getStatus();

        if (ator instanceof Gestor) {
            if (status != StatusOcorrencia.ABERTA && status != StatusOcorrencia.DESCARTADA) {
                throw new RegraNegocioException(
                        "Ocorrências já atendidas fazem parte do histórico. Só é possível excluir abertas ou descartadas.");
            }
        } else {
            if (!ocorrencia.getAutor().getId().equals(ator.getId())) {
                throw new AcessoNegadoException("Você só pode excluir ocorrências que você registrou.");
            }
            if (status != StatusOcorrencia.ABERTA) {
                throw new RegraNegocioException("A gestão já está cuidando desta ocorrência, por isso ela não pode ser excluída.");
            }
        }
        ocorrenciaDao.excluir(id);
    }

    /** Muda o status e registra a ação do gestor no histórico, na mesma transação. */
    public Ocorrencia alterarStatus(long id, StatusOcorrencia novoStatus, String descricao, Long idAtor) {
        Gestor gestor = autenticacao.exigirGestor(idAtor, "alterar o status de ocorrências");

        Validador validador = new Validador();
        validador.obrigatorio("status", novoStatus, "Escolha o novo status.");
        String descricaoLimpa = validador.texto("descricao", descricao, "Descrição da ação", 5, 500);
        validador.validar();

        Ocorrencia ocorrencia = buscar(id);
        StatusOcorrencia anterior = ocorrencia.getStatus();
        if (!podeMudar(anterior, novoStatus)) {
            throw new RegraNegocioException("Não é possível passar de \"" + anterior.getRotulo() + "\" para \""
                    + novoStatus.getRotulo() + "\".");
        }

        LocalDateTime agora = LocalDateTime.now();
        ocorrencia.setStatus(novoStatus);
        ocorrencia.setDataAtualizacao(agora);
        Acao acao = new Acao(id, gestor, descricaoLimpa, novoStatus);
        acao.setDataAcao(agora);

        if (!ocorrenciaDao.alterarStatus(ocorrencia, anterior, acao)) {
            throw new RegraNegocioException("Outro gestor acabou de alterar esta ocorrência. Atualize a página e confira.");
        }
        return ocorrencia;
    }

    private Ocorrencia validar(Ocorrencia dados) {
        Validador validador = new Validador();
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setTitulo(validador.texto("titulo", dados.getTitulo(), "Título", 5, 100));
        ocorrencia.setDescricao(validador.texto("descricao", dados.getDescricao(), "Descrição", 10, 500));
        validador.obrigatorio("tipo", dados.getTipo(), "Escolha o tipo de ocorrência.");
        ocorrencia.setTipo(dados.getTipo());
        validador.intervalo("gravidade", dados.getGravidade(), "Gravidade", 1, 5);
        ocorrencia.setGravidade(dados.getGravidade());

        List<OcorrenciaVia> vias = dados.getVias();
        if (vias.isEmpty()) {
            validador.erro("vias", "Informe pelo menos uma via afetada.");
        } else if (vias.size() > MAXIMO_VIAS) {
            validador.erro("vias", "Informe no máximo " + MAXIMO_VIAS + " vias.");
        }
        Set<Long> repetidas = new HashSet<>();
        for (OcorrenciaVia item : vias) {
            if (item.getVia() == null || item.getVia().getId() == null) {
                validador.erro("vias", "Escolha a via em todas as linhas.");
            } else if (!repetidas.add(item.getVia().getId())) {
                validador.erro("vias", "A mesma via foi escolhida mais de uma vez.");
            }
            if (item.getNivelImpacto() == null) {
                validador.erro("vias", "Escolha o nível de impacto de cada via.");
            }
        }
        validador.validar();

        List<OcorrenciaVia> carregadas = new ArrayList<>();
        for (OcorrenciaVia item : vias) {
            long idVia = item.getVia().getId();
            Via via = viaDao.buscarPorId(idVia).orElseThrow(() -> new ValidacaoException(
                    "A via " + idVia + " não existe.", Map.of("vias", "A via escolhida não existe mais.")));
            carregadas.add(new OcorrenciaVia(via, item.getNivelImpacto()));
        }
        ocorrencia.setVias(carregadas);
        return ocorrencia;
    }
}
