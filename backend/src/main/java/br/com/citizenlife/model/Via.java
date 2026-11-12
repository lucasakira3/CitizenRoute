package br.com.citizenlife.model;

/** Avenida ou rua cadastrada, com a zona da cidade (tabela CL_VIA). */
public class Via {

    private Long id;
    private String nome;
    private String zona;

    public Via() {
    }

    public Via(Long id, String nome, String zona) {
        this.id = id;
        this.nome = nome;
        this.zona = zona;
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

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }
}
