package br.com.citizenroute.model;

import java.time.LocalDateTime;

/**
 * Base de todos os usuários (tabela CR_USUARIO).
 * É abstrata: no sistema só existem cidadãos e gestores, nunca um "usuário genérico".
 */
public abstract class Usuario {

    private Long id;
    private String nome;
    private String email;
    private String senhaHash;
    private LocalDateTime dataCadastro;

    protected Usuario() {
    }

    protected Usuario(Long id, String nome, String email) {
        this.id = id;
        this.nome = nome;
        this.email = email;
    }

    /** Cada subclasse informa o próprio perfil. */
    public abstract TipoUsuario getTipo();

    /** Texto curto exibido na interface, por exemplo "Cidadão · Moto". */
    public abstract String getDescricaoPerfil();

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

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }
}
