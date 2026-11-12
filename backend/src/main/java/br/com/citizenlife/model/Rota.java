package br.com.citizenlife.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Caminho salvo pelo cidadão, da saída ao destino (tabela CL_ROTA).
 * Guarda as vias cadastradas por onde passa (tabela associativa CL_ROTA_VIA, N:N com CL_VIA).
 * X e Y são a posição no mapa, usada para redesenhar o caminho.
 */
public class Rota {

    private Long id;
    private Long idCidadao;
    private String nome;
    private String origemDescricao;
    private double origemX;
    private double origemY;
    private String destinoDescricao;
    private double destinoX;
    private double destinoY;
    private int distanciaMetros;
    private LocalDateTime dataCriacao;
    private List<Via> vias = new ArrayList<>();

    /** Indica se a rota passa pela via informada. */
    public boolean passaPor(Long idVia) {
        return vias.stream().anyMatch(via -> via.getId().equals(idVia));
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getOrigemDescricao() {
        return origemDescricao;
    }

    public void setOrigemDescricao(String origemDescricao) {
        this.origemDescricao = origemDescricao;
    }

    public double getOrigemX() {
        return origemX;
    }

    public void setOrigemX(double origemX) {
        this.origemX = origemX;
    }

    public double getOrigemY() {
        return origemY;
    }

    public void setOrigemY(double origemY) {
        this.origemY = origemY;
    }

    public String getDestinoDescricao() {
        return destinoDescricao;
    }

    public void setDestinoDescricao(String destinoDescricao) {
        this.destinoDescricao = destinoDescricao;
    }

    public double getDestinoX() {
        return destinoX;
    }

    public void setDestinoX(double destinoX) {
        this.destinoX = destinoX;
    }

    public double getDestinoY() {
        return destinoY;
    }

    public void setDestinoY(double destinoY) {
        this.destinoY = destinoY;
    }

    public int getDistanciaMetros() {
        return distanciaMetros;
    }

    public void setDistanciaMetros(int distanciaMetros) {
        this.distanciaMetros = distanciaMetros;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public List<Via> getVias() {
        return vias;
    }

    public void setVias(List<Via> vias) {
        this.vias = vias == null ? new ArrayList<>() : new ArrayList<>(vias);
    }
}
