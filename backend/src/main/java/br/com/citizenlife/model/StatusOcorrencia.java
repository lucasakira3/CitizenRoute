package br.com.citizenlife.model;

/**
 * Ciclo de vida de uma ocorrência.
 * As ativas aparecem no mapa de riscos e geram alertas nas rotas.
 */
public enum StatusOcorrencia {
    ABERTA("Aberta", true),
    CONFIRMADA("Confirmada", true),
    EM_ATENDIMENTO("Em atendimento", true),
    RESOLVIDA("Resolvida", false),
    DESCARTADA("Descartada", false);

    private final String rotulo;
    private final boolean ativa;

    StatusOcorrencia(String rotulo, boolean ativa) {
        this.rotulo = rotulo;
        this.ativa = ativa;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Uma ocorrência ativa ainda afeta a via e pode gerar alertas. */
    public boolean isAtiva() {
        return ativa;
    }
}
