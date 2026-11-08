package br.com.citizenroute.model;

/** Quem circula pela cidade e reporta ocorrências (tabela CR_CIDADAO). */
public class Cidadao extends Usuario {

    private Modal modal;

    public Cidadao() {
    }

    public Cidadao(Long id, String nome, String email, Modal modal) {
        super(id, nome, email);
        this.modal = modal;
    }

    @Override
    public TipoUsuario getTipo() {
        return TipoUsuario.CIDADAO;
    }

    @Override
    public String getDescricaoPerfil() {
        return "Cidadão · " + (modal == null ? "sem modal" : modal.getRotulo());
    }

    public Modal getModal() {
        return modal;
    }

    public void setModal(Modal modal) {
        this.modal = modal;
    }
}
