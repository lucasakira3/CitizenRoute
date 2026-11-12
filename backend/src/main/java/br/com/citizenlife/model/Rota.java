package br.com.citizenlife.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Rota salva pelo cidadão, como "Casa → Trabalho" (tabela CL_ROTA).
 *
 * <p>A lista de vias (tabela associativa CL_ROTA_VIA, N:N entre rota e via) guarda, na ordem
 * do caminho, as vias cadastradas que a rota percorre. É por elas que os alertas são calculados:
 * uma ocorrência numa dessas vias vira alerta para o dono da rota.</p>
 */
public class Rota {

    private Long id;
    private Long idCidadao;
    private String nome;
    private PontoRota origem;
    private PontoRota destino;
    private CriterioRota criterio;
    private int distanciaMetros;
    private LocalDateTime dataCriacao;
    private List<Via> vias = new ArrayList<>();

    public Rota() {
    }

    public Rota(Long id, Long idCidadao, String nome, PontoRota origem, PontoRota destino,
                CriterioRota criterio, int distanciaMetros) {
        this.id = id;
        this.idCidadao = idCidadao;
        this.nome = nome;
        this.origem = origem;
        this.destino = destino;
        this.criterio = criterio;
        this.distanciaMetros = distanciaMetros;
    }

    /** Verdadeiro quando a rota passa pela via informada. */
    public boolean passaPelaVia(Long idVia) {
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

    public PontoRota getOrigem() {
        return origem;
    }

    public void setOrigem(PontoRota origem) {
        this.origem = origem;
    }

    public PontoRota getDestino() {
        return destino;
    }

    public void setDestino(PontoRota destino) {
        this.destino = destino;
    }

    public CriterioRota getCriterio() {
        return criterio;
    }

    public void setCriterio(CriterioRota criterio) {
        this.criterio = criterio;
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
        this.vias = vias;
    }
}
