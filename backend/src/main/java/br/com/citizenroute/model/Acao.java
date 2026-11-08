package br.com.citizenroute.model;

import java.time.LocalDateTime;

/**
 * Resposta de um gestor a uma ocorrência (tabela CR_ACAO).
 * Quando a ação muda o status, statusNovo guarda o status resultante.
 * Quando é só um registro de andamento, statusNovo fica nulo.
 */
public class Acao {

    private Long id;
    private Long idOcorrencia;
    private Gestor gestor;
    private String descricao;
    private StatusOcorrencia statusNovo;
    private LocalDateTime dataAcao;

    public Acao() {
    }

    public Acao(Long idOcorrencia, Gestor gestor, String descricao, StatusOcorrencia statusNovo) {
        this.idOcorrencia = idOcorrencia;
        this.gestor = gestor;
        this.descricao = descricao;
        this.statusNovo = statusNovo;
    }

    public boolean isMudancaDeStatus() {
        return statusNovo != null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdOcorrencia() {
        return idOcorrencia;
    }

    public void setIdOcorrencia(Long idOcorrencia) {
        this.idOcorrencia = idOcorrencia;
    }

    public Gestor getGestor() {
        return gestor;
    }

    public void setGestor(Gestor gestor) {
        this.gestor = gestor;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public StatusOcorrencia getStatusNovo() {
        return statusNovo;
    }

    public void setStatusNovo(StatusOcorrencia statusNovo) {
        this.statusNovo = statusNovo;
    }

    public LocalDateTime getDataAcao() {
        return dataAcao;
    }

    public void setDataAcao(LocalDateTime dataAcao) {
        this.dataAcao = dataAcao;
    }
}
