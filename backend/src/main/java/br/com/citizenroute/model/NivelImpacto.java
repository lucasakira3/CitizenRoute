package br.com.citizenroute.model;

/** Quanto uma ocorrência afeta uma via específica (CR_OCORRENCIA_VIA.NIVEL_IMPACTO). */
public enum NivelImpacto {
    BLOQUEIO_TOTAL("Bloqueio total"),
    PARCIAL("Bloqueio parcial"),
    LENTIDAO("Lentidão"),
    SEM_INTERDICAO("Sem interdição");

    private final String rotulo;

    NivelImpacto(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
