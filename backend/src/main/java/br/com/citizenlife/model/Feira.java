package br.com.citizenlife.model;

/** Feira livre do bairro ou da zona (tabela CL_FEIRA). */
public class Feira {

    private Long id;
    private String nome;
    private String rua;
    private String zona;
    private String diaSemana;
    private String horario;

    public Feira() {
    }

    public Feira(Long id, String nome, String rua, String zona, String diaSemana, String horario) {
        this.id = id;
        this.nome = nome;
        this.rua = rua;
        this.zona = zona;
        this.diaSemana = diaSemana;
        this.horario = horario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getRua() {
        return rua;
    }

    public void setRua(String rua) {
        this.rua = rua;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public String getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(String diaSemana) {
        this.diaSemana = diaSemana;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }
}
