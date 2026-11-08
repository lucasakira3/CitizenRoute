package br.com.citizenroute.model;

/** Faixas de risco. Os limites numéricos de cada faixa ficam em RiscoService. */
public enum NivelRisco {
    BAIXO("Baixo"),
    MODERADO("Moderado"),
    ALTO("Alto"),
    CRITICO("Crítico");

    private final String rotulo;

    NivelRisco(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
