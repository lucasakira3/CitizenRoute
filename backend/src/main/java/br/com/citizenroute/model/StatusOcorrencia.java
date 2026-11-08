package br.com.citizenroute.model;

/**
 * Ciclo de vida de uma ocorrência.
 * As transições permitidas entre os status ficam em OcorrenciaService.
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
