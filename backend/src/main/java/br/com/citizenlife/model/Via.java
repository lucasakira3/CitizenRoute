package br.com.citizenlife.model;

/** Avenida, rua ou trecho, sempre ligado a uma região (tabela CL_VIA). */
public class Via {

    private Long id;
    private String nome;
    private Regiao regiao;

    public Via() {
    }

    public Via(Long id, String nome, Regiao regiao) {
        this.id = id;
        this.nome = nome;
        this.regiao = regiao;
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

    public Regiao getRegiao() {
        return regiao;
    }

    public void setRegiao(Regiao regiao) {
        this.regiao = regiao;
    }
}
