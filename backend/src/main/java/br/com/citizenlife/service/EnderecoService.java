package br.com.citizenlife.service;

import br.com.citizenlife.dao.EnderecoDao;
import br.com.citizenlife.exception.CitizenLifeException;
import br.com.citizenlife.exception.ValidacaoException;
import br.com.citizenlife.model.Endereco;

import java.util.List;
import java.util.Objects;

/**
 * Endereços salvos do cidadão. Regras: apelido sem repetir, uma casa e um trabalho no máximo
 * e até {@value #MAXIMO_ENDERECOS} endereços por pessoa.
 */
public class EnderecoService {

    public static final int MAXIMO_ENDERECOS = 10;

    private final EnderecoDao enderecoDao;
    private final CidadaoService cidadaoService;

    public EnderecoService(EnderecoDao enderecoDao, CidadaoService cidadaoService) {
        this.enderecoDao = enderecoDao;
        this.cidadaoService = cidadaoService;
    }

    public List<Endereco> listar(long idCidadao, Long idAtor) {
        cidadaoService.exigirDono(idAtor, idCidadao);
        return enderecoDao.listarPorCidadao(idCidadao);
    }

    public Endereco criar(long idCidadao, Endereco dados, Long idAtor) {
        cidadaoService.exigirDono(idAtor, idCidadao);
        List<Endereco> atuais = enderecoDao.listarPorCidadao(idCidadao);
        validar(dados, atuais);
        if (atuais.size() >= MAXIMO_ENDERECOS) {
            throw new ValidacaoException("apelido", "Você já tem " + MAXIMO_ENDERECOS + " endereços. Exclua um antes.");
        }
        dados.setIdCidadao(idCidadao);
        return enderecoDao.inserir(dados);
    }

    public Endereco atualizar(long idCidadao, long idEndereco, Endereco dados, Long idAtor) {
        cidadaoService.exigirDono(idAtor, idCidadao);
        enderecoDoCidadao(idCidadao, idEndereco);
        dados.setId(idEndereco);
        dados.setIdCidadao(idCidadao);
        validar(dados, enderecoDao.listarPorCidadao(idCidadao));
        enderecoDao.atualizar(dados);
        return dados;
    }

    public void excluir(long idCidadao, long idEndereco, Long idAtor) {
        cidadaoService.exigirDono(idAtor, idCidadao);
        enderecoDoCidadao(idCidadao, idEndereco);
        enderecoDao.excluir(idEndereco);
    }

    private void validar(Endereco endereco, List<Endereco> atuais) {
        ValidacaoException erros = new ValidacaoException();
        endereco.setApelido(erros.texto("apelido", endereco.getApelido(), "Apelido", 2, 40));
        endereco.setDescricao(erros.texto("local", endereco.getDescricao(), "Local", 2, 120));
        erros.exigir(Endereco.TIPOS.contains(endereco.getTipo()), "tipo", "Escolha o tipo do endereço.");
        erros.exigir(endereco.getX() >= 0 && endereco.getY() >= 0, "local", "Escolha o local no mapa.");
        for (Endereco outro : atuais) {
            if (Objects.equals(outro.getId(), endereco.getId())) {
                continue;
            }
            erros.exigir(!(endereco.isTipoUnico() && outro.getTipo().equals(endereco.getTipo())),
                    "tipo", "Você já tem um endereço deste tipo.");
            erros.exigir(!outro.getApelido().equalsIgnoreCase(endereco.getApelido()),
                    "apelido", "Você já tem um endereço com este apelido.");
        }
        erros.lancarSeHouverErros();
    }

    private Endereco enderecoDoCidadao(long idCidadao, long idEndereco) {
        return enderecoDao.buscarPorId(idEndereco)
                .filter(endereco -> endereco.getIdCidadao() == idCidadao)
                .orElseThrow(() -> CitizenLifeException.naoEncontrado("Endereço não encontrado."));
    }
}
