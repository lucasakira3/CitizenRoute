package br.com.citizenlife.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Evento reportado por um cidadão que afeta uma ou mais vias (tabela CL_OCORRENCIA). */
public class Ocorrencia implements AvaliavelRisco {

    private Long id;
    private String titulo;
    private String descricao;
    private TipoOcorrencia tipo;
    private int gravidade;
    private StatusOcorrencia status;
    private LocalDateTime dataRegistro;
    private LocalDateTime dataAtualizacao;
    private Cidadao autor;
    private List<OcorrenciaVia> vias = new ArrayList<>();

    /**
     * Regra principal do projeto: risco = gravidade × peso do tipo × fator do modal.
     * Um alagamento de gravidade 3 vale 3,6 para quem está de carro e 8,1 para quem está de moto.
     */
    @Override
    public double calcularRisco(Modal modal) {
        return gravidade * tipo.getPeso() * tipo.fatorExposicao(modal);
    }

    public boolean isAtiva() {
        return status != null && status.isAtiva();
    }

    /** Indica se a ocorrência atinge a via informada. */
    public boolean afetaVia(Long idVia) {
        return vias.stream().anyMatch(item -> item.getVia().getId().equals(idVia));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public TipoOcorrencia getTipo() {
        return tipo;
    }

    public void setTipo(TipoOcorrencia tipo) {
        this.tipo = tipo;
    }

    public int getGravidade() {
        return gravidade;
    }

    public void setGravidade(int gravidade) {
        this.gravidade = gravidade;
    }

    public StatusOcorrencia getStatus() {
        return status;
    }

    public void setStatus(StatusOcorrencia status) {
        this.status = status;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDateTime dataRegistro) {
        this.dataRegistro = dataRegistro;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    public Cidadao getAutor() {
        return autor;
    }

    public void setAutor(Cidadao autor) {
        this.autor = autor;
    }

    public List<OcorrenciaVia> getVias() {
        return vias;
    }

    public void setVias(List<OcorrenciaVia> vias) {
        this.vias = vias == null ? new ArrayList<>() : new ArrayList<>(vias);
    }
}
