package br.com.citizenlife.model;

import java.time.LocalDateTime;

/**
 * Endereço salvo pelo cidadão, como "Casa" ou "Trabalho" (tabela CL_ENDERECO).
 * Serve de atalho para a saída e o destino na tela de rotas.
 * O local usa o mesmo {@link PontoRota} das rotas: o nome escolhido e a posição no mapa.
 */
public class Endereco {

    private Long id;
    private Long idCidadao;
    private String apelido;
    private TipoEndereco tipo;
    private PontoRota local;
    private LocalDateTime dataCriacao;

    public Endereco() {
    }

    public Endereco(Long id, Long idCidadao, String apelido, TipoEndereco tipo, PontoRota local) {
        this.id = id;
        this.idCidadao = idCidadao;
        this.apelido = apelido;
        this.tipo = tipo;
        this.local = local;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdCidadao() {
        return idCidadao;
    }

    public void setIdCidadao(Long idCidadao) {
        this.idCidadao = idCidadao;
    }

    public String getApelido() {
        return apelido;
    }

    public void setApelido(String apelido) {
        this.apelido = apelido;
    }

    public TipoEndereco getTipo() {
        return tipo;
    }

    public void setTipo(TipoEndereco tipo) {
        this.tipo = tipo;
    }

    public PontoRota getLocal() {
        return local;
    }

    public void setLocal(PontoRota local) {
        this.local = local;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
