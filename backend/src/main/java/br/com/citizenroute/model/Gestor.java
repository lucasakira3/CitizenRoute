package br.com.citizenroute.model;

/** Servidor de um órgão público que confirma ocorrências e registra ações (tabela CR_GESTOR). */
public class Gestor extends Usuario {

    private Orgao orgao;
    private String matricula;

    public Gestor() {
    }

    public Gestor(Long id, String nome, String email, Orgao orgao, String matricula) {
        super(id, nome, email);
        this.orgao = orgao;
        this.matricula = matricula;
    }

    @Override
    public TipoUsuario getTipo() {
        return TipoUsuario.GESTOR;
    }

    @Override
    public String getDescricaoPerfil() {
        return "Gestor · " + (orgao == null ? "sem órgão" : orgao.getRotulo());
    }

    public Orgao getOrgao() {
        return orgao;
    }

    public void setOrgao(Orgao orgao) {
        this.orgao = orgao;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }
}
