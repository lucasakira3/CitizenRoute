package br.com.citizenlife.model;

import java.util.List;

/**
 * Lugar salvo pelo cidadão, como "Casa" ou "Trabalho" (tabela CL_ENDERECO).
 * Vira atalho de saída e destino na tela de rota.
 */
public class Endereco {

    public static final List<String> TIPOS = List.of("CASA", "TRABALHO", "ESTUDO", "OUTRO");

    private Long id;
    private Long idCidadao;
    private String apelido;
    private String tipo;
    private String descricao;
    private double x;
    private double y;

    /** Casa e trabalho são únicos: cada cidadão tem no máximo um de cada. */
    public boolean isTipoUnico() {
        return "CASA".equals(tipo) || "TRABALHO".equals(tipo);
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
