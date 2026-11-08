package br.com.citizenroute.model;

import java.time.LocalDateTime;

/**
 * Associação N:N entre cidadão e via (tabela CR_VIA_MONITORADA).
 * Representa um trajeto salvo, como "caminho do trabalho".
 */
public class ViaMonitorada {

    private Long idCidadao;
    private Via via;
    private String apelido;
    private LocalDateTime dataInclusao;

    public ViaMonitorada() {
    }

    public ViaMonitorada(Long idCidadao, Via via, String apelido) {
        this.idCidadao = idCidadao;
        this.via = via;
        this.apelido = apelido;
    }

    public Long getIdCidadao() {
        return idCidadao;
    }

    public void setIdCidadao(Long idCidadao) {
        this.idCidadao = idCidadao;
    }

    public Via getVia() {
        return via;
    }

    public void setVia(Via via) {
        this.via = via;
    }

    public String getApelido() {
        return apelido;
    }

    public void setApelido(String apelido) {
        this.apelido = apelido;
    }

    public LocalDateTime getDataInclusao() {
        return dataInclusao;
    }

    public void setDataInclusao(LocalDateTime dataInclusao) {
        this.dataInclusao = dataInclusao;
    }
}
