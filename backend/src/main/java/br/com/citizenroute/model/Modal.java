package br.com.citizenroute.model;

/**
 * Meio de transporte que o cidadão usa no dia a dia.
 * Motorista, motociclista, passageiro e pedestre são todos {@link Cidadao};
 * o que muda entre eles é o modal, e o modal muda o risco de cada ocorrência.
 */
public enum Modal {
    CARRO("Carro"),
    MOTO("Moto"),
    ONIBUS("Passageiro de ônibus"),
    APP("Motorista de app"),
    PEDESTRE("Pedestre"),
    BIKE("Bicicleta");

    private final String rotulo;

    Modal(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
