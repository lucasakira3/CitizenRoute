package br.com.citizenlife.model;

/**
 * Tipo de um endereço salvo pelo cidadão (CL_ENDERECO.TIPO).
 * Casa e trabalho são únicos por cidadão; estudo e outro podem se repetir.
 */
public enum TipoEndereco {
    CASA("Casa", true),
    TRABALHO("Trabalho", true),
    ESTUDO("Estudo", false),
    OUTRO("Outro", false);

    private final String rotulo;
    private final boolean unico;

    TipoEndereco(String rotulo, boolean unico) {
        this.rotulo = rotulo;
        this.unico = unico;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Verdadeiro quando o cidadão só pode ter um endereço deste tipo. */
    public boolean isUnico() {
        return unico;
    }
}
