package br.com.citizenroute.service;

import br.com.citizenroute.dao.AcaoDao;
import br.com.citizenroute.exception.AcessoNegadoException;
import br.com.citizenroute.exception.RecursoNaoEncontradoException;
import br.com.citizenroute.exception.RegraNegocioException;
import br.com.citizenroute.model.Acao;
import br.com.citizenroute.model.Gestor;
import br.com.citizenroute.model.Ocorrencia;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Registros de andamento feitos pelos gestores.
 * As mudanças de status também geram ações, mas essas são criadas pela OcorrenciaService.
 */
public class AcaoService {

    private final AcaoDao acaoDao;
    private final OcorrenciaService ocorrenciaService;
    private final AutenticacaoService autenticacao;

    public AcaoService(AcaoDao acaoDao, OcorrenciaService ocorrenciaService, AutenticacaoService autenticacao) {
        this.acaoDao = acaoDao;
        this.ocorrenciaService = ocorrenciaService;
        this.autenticacao = autenticacao;
    }

    public List<Acao> listarPorOcorrencia(long idOcorrencia) {
        ocorrenciaService.buscar(idOcorrencia);
        return acaoDao.listarPorOcorrencia(idOcorrencia);
    }

    public Acao buscar(long id) {
        return acaoDao.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ação " + id + " não encontrada."));
    }

    /** Registra um andamento sem mudar o status, por exemplo "equipe a caminho". */
    public Acao registrar(long idOcorrencia, String descricao, Long idAtor) {
        Gestor gestor = autenticacao.exigirGestor(idAtor, "registrar ações");
        String descricaoLimpa = validarDescricao(descricao);
        Ocorrencia ocorrencia = ocorrenciaService.buscar(idOcorrencia);
        if (!ocorrencia.isAtiva()) {
            throw new RegraNegocioException("Esta ocorrência já foi encerrada. Não é possível registrar novas ações.");
        }

        Acao acao = new Acao(idOcorrencia, gestor, descricaoLimpa, null);
        acao.setDataAcao(LocalDateTime.now());
        return acaoDao.inserir(acao);
    }

    public Acao atualizar(long id, String descricao, Long idAtor) {
        Gestor gestor = autenticacao.exigirGestor(idAtor, "editar ações");
        Acao acao = buscar(id);
        exigirAutor(acao, gestor, "editar");
        acao.setDescricao(validarDescricao(descricao));
        if (!acaoDao.atualizar(acao)) {
            throw new RecursoNaoEncontradoException("Ação " + id + " não encontrada.");
        }
        return acao;
    }

    public void excluir(long id, Long idAtor) {
        Gestor gestor = autenticacao.exigirGestor(idAtor, "excluir ações");
        Acao acao = buscar(id);
        exigirAutor(acao, gestor, "excluir");
        if (acao.isMudancaDeStatus()) {
            throw new RegraNegocioException("Ações que mudaram o status fazem parte do histórico e não podem ser excluídas.");
        }
        acaoDao.excluir(id);
    }

    private void exigirAutor(Acao acao, Gestor gestor, String verbo) {
        if (!acao.getGestor().getId().equals(gestor.getId())) {
            throw new AcessoNegadoException("Só o gestor que registrou a ação pode " + verbo + " o registro.");
        }
    }

    private String validarDescricao(String descricao) {
        Validador validador = new Validador();
        String limpa = validador.texto("descricao", descricao, "Descrição da ação", 5, 500);
        validador.validar();
        return limpa;
    }
}
