package br.com.citizenlife.model;

/** Zona da cidade, como Centro ou Zona Leste (tabela CL_REGIAO). */
public class Regiao {

    private Long id;
    private String nome;

    public Regiao() {
    }

    public Regiao(Long id, String nome) {
        this.id = id;
        this.nome = nome;
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
}
