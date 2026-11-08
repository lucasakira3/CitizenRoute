package br.com.citizenroute.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Tipos de ocorrência e a matriz de risco por modal.
 *
 * <p>Cada tipo tem um peso base e um fator de exposição para cada modal.
 * É aqui que fica registrado que o mesmo alagamento é lentidão para o carro
 * (fator 0.8) e perigo para a moto ou o pedestre (fator 1.8).</p>
 */
public enum TipoOcorrencia {
    //                 rótulo                    peso  órgão responsável      CARRO MOTO ÔNIBUS APP  PEDESTRE BIKE
    ACIDENTE("Acidente de trânsito",             1.5, Orgao.CET,             1.0, 1.6, 0.8, 1.0, 1.2, 1.5),
    ALAGAMENTO("Alagamento",                     1.5, Orgao.DEFESA_CIVIL,    0.8, 1.8, 0.6, 0.8, 1.8, 1.6),
    QUEDA_ARVORE("Queda de árvore",              1.3, Orgao.SUBPREFEITURA,   1.0, 1.5, 0.8, 1.0, 1.4, 1.5),
    SEMAFORO_APAGADO("Semáforo apagado",         1.0, Orgao.CET,             1.0, 1.3, 0.8, 1.0, 1.6, 1.4),
    DESLIZAMENTO("Deslizamento de terra",        2.0, Orgao.DEFESA_CIVIL,    1.2, 1.6, 1.0, 1.2, 1.8, 1.6),
    VIA_BLOQUEADA("Via bloqueada",               1.0, Orgao.CET,             0.8, 0.7, 1.0, 0.9, 0.5, 0.6),
    INSEGURANCA("Insegurança no trajeto",        1.2, Orgao.GCM,             0.6, 1.2, 1.5, 1.3, 1.8, 1.4);

    private final String rotulo;
    private final double peso;
    private final Orgao orgaoResponsavel;
    private final Map<Modal, Double> fatorPorModal;

    TipoOcorrencia(String rotulo, double peso, Orgao orgaoResponsavel,
                   double carro, double moto, double onibus, double app, double pedestre, double bike) {
        this.rotulo = rotulo;
        this.peso = peso;
        this.orgaoResponsavel = orgaoResponsavel;

        Map<Modal, Double> fatores = new EnumMap<>(Modal.class);
        fatores.put(Modal.CARRO, carro);
        fatores.put(Modal.MOTO, moto);
        fatores.put(Modal.ONIBUS, onibus);
        fatores.put(Modal.APP, app);
        fatores.put(Modal.PEDESTRE, pedestre);
        fatores.put(Modal.BIKE, bike);
        this.fatorPorModal = Collections.unmodifiableMap(fatores);
    }

    public String getRotulo() {
        return rotulo;
    }

    public double getPeso() {
        return peso;
    }

    /** Órgão que normalmente atende este tipo; usado para montar a fila de cada gestor. */
    public Orgao getOrgaoResponsavel() {
        return orgaoResponsavel;
    }

    /** Fator de exposição do modal a este tipo de ocorrência (de 0.5 a 1.8). */
    public double fatorExposicao(Modal modal) {
        return fatorPorModal.get(modal);
    }
}
