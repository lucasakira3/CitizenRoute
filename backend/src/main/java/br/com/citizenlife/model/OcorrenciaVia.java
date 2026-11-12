package br.com.citizenlife.model;

/**
 * Associação N:N entre ocorrência e via (tabela CL_OCORRENCIA_VIA).
 * Uma ocorrência pode atingir várias vias, cada uma com um nível de impacto.
 */
public class OcorrenciaVia {

    private Via via;
    private NivelImpacto nivelImpacto;

    public OcorrenciaVia() {
    }

    public OcorrenciaVia(Via via, NivelImpacto nivelImpacto) {
        this.via = via;
        this.nivelImpacto = nivelImpacto;
    }

    public Via getVia() {
        return via;
    }

    public void setVia(Via via) {
        this.via = via;
    }

    public NivelImpacto getNivelImpacto() {
        return nivelImpacto;
    }

    public void setNivelImpacto(NivelImpacto nivelImpacto) {
        this.nivelImpacto = nivelImpacto;
    }
}
