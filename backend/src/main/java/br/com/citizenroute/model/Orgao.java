package br.com.citizenroute.model;

/** Órgão público ao qual o gestor pertence. */
public enum Orgao {
    CET("CET", "Trânsito e sinalização"),
    DEFESA_CIVIL("Defesa Civil", "Enchentes, deslizamentos e riscos climáticos"),
    SPTRANS("SPTrans", "Transporte público por ônibus"),
    SUBPREFEITURA("Subprefeitura", "Manutenção urbana e áreas verdes"),
    GCM("Guarda Civil Metropolitana", "Segurança em pontos e trajetos");

    private final String rotulo;
    private final String atribuicao;

    Orgao(String rotulo, String atribuicao) {
        this.rotulo = rotulo;
        this.atribuicao = atribuicao;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getAtribuicao() {
        return atribuicao;
    }
}
