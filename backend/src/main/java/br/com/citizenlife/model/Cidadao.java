package br.com.citizenlife.model;

import java.time.LocalDateTime;

/**
 * Quem usa o Citizen Life (tabela CL_CIDADAO).
 * O modal (como a pessoa se locomove) muda o risco de cada ocorrência no caminho.
 * O cadastro não pede o modal: ele é configurado depois, no perfil.
 */
public class Cidadao {

    private Long id;
    private String nome;
    private String email;
    private String senhaHash;
    private Modal modal;
    private LocalDateTime dataCadastro;

    public Cidadao() {
    }

    public Cidadao(Long id, String nome, String email, Modal modal) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.modal = modal;
    }

    /** Modal usado no cálculo do risco quando a pessoa ainda não informou o dela. */
    public static final Modal MODAL_PADRAO_RISCO = Modal.PEDESTRE;

    /** Texto curto exibido na interface, por exemplo "Cidadão · Moto". */
    public String getDescricaoPerfil() {
        return modal == null ? "Cidadão" : "Cidadão · " + modal.getRotulo();
    }

    /**
     * Modal para calcular o risco dos alertas. Sem modal informado, calcula para quem anda a pé,
     * o caso mais exposto na maioria das ocorrências: na dúvida, o sistema avisa mais, e não menos.
     */
    public Modal modalParaRisco() {
        return modal == null ? MODAL_PADRAO_RISCO : modal;
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

    public Modal getModal() {
        return modal;
    }

    public void setModal(Modal modal) {
        this.modal = modal;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }
}
