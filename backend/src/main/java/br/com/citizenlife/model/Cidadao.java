package br.com.citizenlife.model;

import java.time.LocalDateTime;

/** Quem usa o Citizen Life (tabela CL_CIDADAO). */
public class Cidadao {

    /** Sem meio de transporte informado, o risco é calculado para quem anda a pé (o caso mais exposto). */
    public static final String MODAL_PADRAO = "PEDESTRE";

    private Long id;
    private String nome;
    private String email;
    private String senhaHash;
    private String modal;
    private LocalDateTime dataCadastro;

    public Cidadao() {
    }

    public Cidadao(Long id, String nome, String email, String modal) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.modal = modal;
    }

    /** Modal usado no cálculo do risco dos alertas. */
    public String modalParaRisco() {
        return modal == null ? MODAL_PADRAO : modal;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getModal() {
        return modal;
    }

    public void setModal(String modal) {
        this.modal = modal;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }
}
