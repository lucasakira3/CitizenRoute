package br.com.citizenlife.model;

/** Como a rota foi escolhida (CL_ROTA.CRITERIO). */
public enum CriterioRota {
    RAPIDA("Mais rápida", "O caminho mais curto em tempo, sem olhar as ocorrências."),
    SEGURA("Mais segura", "Desvia das vias com risco moderado ou maior para o modal, mesmo que fique mais longa.");

    private final String rotulo;
    private final String descricao;

    CriterioRota(String rotulo, String descricao) {
        this.rotulo = rotulo;
        this.descricao = descricao;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getDescricao() {
        return descricao;
    }
}
