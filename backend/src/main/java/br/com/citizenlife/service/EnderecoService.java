package br.com.citizenlife.service;

import br.com.citizenlife.dao.EnderecoDao;
import br.com.citizenlife.exception.RecursoNaoEncontradoException;
import br.com.citizenlife.exception.RegraNegocioException;
import br.com.citizenlife.model.Endereco;
import br.com.citizenlife.model.PontoRota;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Endereços salvos pelo cidadão (casa, trabalho, estudo...). Cada cidadão só vê e altera os próprios.
 *
 * <ul>
 *   <li>O apelido é único por cidadão (o banco garante com UK_CL_ENDERECO_APELIDO).</li>
 *   <li>Casa e trabalho são únicos: dá para ter vários "Outro", mas só uma "Casa".</li>
 *   <li>No máximo {@value #MAXIMO_ENDERECOS} endereços por cidadão.</li>
 * </ul>
 */
public class EnderecoService {

    public static final int MAXIMO_ENDERECOS = 10;

    private final EnderecoDao enderecoDao;
    private final AutenticacaoService autenticacao;

    public EnderecoService(EnderecoDao enderecoDao, AutenticacaoService autenticacao) {
        this.enderecoDao = enderecoDao;
        this.autenticacao = autenticacao;
    }

    public List<Endereco> listar(long idCidadao, Long idAtor) {
        autenticacao.exigirProprio(idAtor, idCidadao, "ver os endereços");
        return enderecoDao.listarPorCidadao(idCidadao);
    }

    public Endereco criar(long idCidadao, Endereco dados, Long idAtor) {
        autenticacao.exigirProprio(idAtor, idCidadao, "salvar endereços");
        List<Endereco> atuais = enderecoDao.listarPorCidadao(idCidadao);
        Endereco endereco = validar(dados, atuais, null);
        if (atuais.size() >= MAXIMO_ENDERECOS) {
            throw new RegraNegocioException("Você já tem " + MAXIMO_ENDERECOS
                    + " endereços salvos. Exclua um antes de criar outro.");
        }
        endereco.setIdCidadao(idCidadao);
        endereco.setDataCriacao(LocalDateTime.now());
        return enderecoDao.inserir(endereco);
    }

    public Endereco atualizar(long idCidadao, long idEndereco, Endereco dados, Long idAtor) {
        autenticacao.exigirProprio(idAtor, idCidadao, "alterar os endereços");
        Endereco atual = enderecoDoCidadao(idCidadao, idEndereco);
        Endereco endereco = validar(dados, enderecoDao.listarPorCidadao(idCidadao), idEndereco);
        endereco.setId(atual.getId());
        endereco.setIdCidadao(atual.getIdCidadao());
        endereco.setDataCriacao(atual.getDataCriacao());
        if (!enderecoDao.atualizar(endereco)) {
            throw new RecursoNaoEncontradoException("Endereço " + idEndereco + " não encontrado.");
        }
        return endereco;
    }

    public void excluir(long idCidadao, long idEndereco, Long idAtor) {
        autenticacao.exigirProprio(idAtor, idCidadao, "excluir os endereços");
        enderecoDoCidadao(idCidadao, idEndereco);
        if (!enderecoDao.excluir(idEndereco)) {
            throw new RecursoNaoEncontradoException("Endereço " + idEndereco + " não encontrado.");
        }
    }

    /**
     * Valida os campos e as regras de tipo.
     *
     * @param ignorar id do próprio endereço na edição (nulo ao criar)
     */
    private Endereco validar(Endereco dados, List<Endereco> atuais, Long ignorar) {
        Validador validador = new Validador();
        String apelido = validador.texto("apelido", dados.getApelido(), "Apelido", 2, 40);
        validador.obrigatorio("tipo", dados.getTipo(), "Escolha o tipo do endereço.");

        PontoRota local = dados.getLocal();
        String descricao = null;
        if (local == null) {
            validador.erro("local", "Escolha o local do endereço.");
        } else {
            descricao = validador.texto("local", local.descricao(), "Local", 2, 120);
            boolean dentro = local.x() >= 0 && local.y() >= 0
                    && local.x() <= RotaService.LIMITE_PLANO && local.y() <= RotaService.LIMITE_PLANO;
            validador.exigir(dentro, "local", "O local escolhido está fora do mapa.");
        }

        for (Endereco outro : atuais) {
            if (outro.getId().equals(ignorar)) {
                continue;
            }
            if (dados.getTipo() != null && dados.getTipo().isUnico() && outro.getTipo() == dados.getTipo()) {
                validador.erro("tipo", "Você já tem um endereço do tipo " + dados.getTipo().getRotulo().toLowerCase()
                        + ". Edite o que existe ou escolha outro tipo.");
            }
            if (outro.getApelido().equalsIgnoreCase(apelido)) {
                validador.erro("apelido", "Você já tem um endereço com este apelido.");
            }
        }
        validador.validar();

        return new Endereco(null, null, apelido, dados.getTipo(), new PontoRota(descricao, local.x(), local.y()));
    }

    private Endereco enderecoDoCidadao(long idCidadao, long idEndereco) {
        Endereco endereco = enderecoDao.buscarPorId(idEndereco)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço " + idEndereco + " não encontrado."));
        if (endereco.getIdCidadao() != idCidadao) {
            throw new RecursoNaoEncontradoException("Endereço " + idEndereco + " não encontrado.");
        }
        return endereco;
    }
}
