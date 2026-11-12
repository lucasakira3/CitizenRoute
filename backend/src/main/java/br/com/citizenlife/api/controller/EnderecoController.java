package br.com.citizenlife.api.controller;

import br.com.citizenlife.api.Conversor;
import br.com.citizenlife.api.CorpoJson;
import br.com.citizenlife.api.Requisicao;
import br.com.citizenlife.api.Resposta;
import br.com.citizenlife.api.Roteador;
import br.com.citizenlife.model.Endereco;
import br.com.citizenlife.model.PontoRota;
import br.com.citizenlife.model.TipoEndereco;
import br.com.citizenlife.service.EnderecoService;

/**
 * Endereços salvos do cidadão: CRUD em /api/cidadaos/{id}/enderecos.
 * Corpo de criação e edição: {"apelido", "tipo", "local": {"descricao", "x", "y"}}.
 */
public class EnderecoController {

    private final EnderecoService enderecoService;

    public EnderecoController(EnderecoService enderecoService) {
        this.enderecoService = enderecoService;
    }

    public void registrar(Roteador roteador) {
        roteador.get("/api/cidadaos/{id}/enderecos", this::listar)
                .post("/api/cidadaos/{id}/enderecos", this::criar)
                .put("/api/cidadaos/{id}/enderecos/{idEndereco}", this::atualizar)
                .delete("/api/cidadaos/{id}/enderecos/{idEndereco}", this::excluir);
    }

    private Resposta listar(Requisicao requisicao) {
        return Resposta.ok(Conversor.lista(
                enderecoService.listar(requisicao.id("id"), requisicao.idUsuario()), Conversor::endereco));
    }

    private Resposta criar(Requisicao requisicao) {
        Endereco criado = enderecoService.criar(requisicao.id("id"), lerEndereco(requisicao.corpo()),
                requisicao.idUsuario());
        return Resposta.criado(Conversor.endereco(criado));
    }

    private Resposta atualizar(Requisicao requisicao) {
        Endereco endereco = enderecoService.atualizar(requisicao.id("id"), requisicao.id("idEndereco"),
                lerEndereco(requisicao.corpo()), requisicao.idUsuario());
        return Resposta.ok(Conversor.endereco(endereco));
    }

    private Resposta excluir(Requisicao requisicao) {
        enderecoService.excluir(requisicao.id("id"), requisicao.id("idEndereco"), requisicao.idUsuario());
        return Resposta.semConteudo();
    }

    /** Monta o endereço a partir do JSON; a validação de conteúdo fica na EnderecoService. */
    private Endereco lerEndereco(CorpoJson corpo) {
        CorpoJson local = corpo.objeto("local");
        PontoRota ponto = null;
        if (local != null) {
            Double x = local.decimal("x");
            Double y = local.decimal("y");
            ponto = new PontoRota(local.texto("descricao"), x == null ? -1 : x, y == null ? -1 : y);
        }
        return new Endereco(null, null, corpo.texto("apelido"), corpo.opcao("tipo", TipoEndereco.class), ponto);
    }
}
