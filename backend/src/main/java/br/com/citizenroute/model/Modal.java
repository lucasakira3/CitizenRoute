package br.com.citizenroute.model;

/**
 * Meio de transporte que o cidadão usa no dia a dia.
 * Motorista, motociclista, passageiro e pedestre são todos {@link Cidadao};
 * o que muda entre eles é o modal, e o modal muda o risco de cada ocorrência.
 *
 * <p>O modal também define como a rota é calculada: a velocidade média estima o tempo
 * da viagem, e pedestres e ciclistas não usam vias expressas nem respeitam a mão única
 * (andam pela calçada ou pela ciclofaixa nos dois sentidos).</p>
 */
public enum Modal {
    CARRO("Carro", 22, true),
    MOTO("Moto", 28, true),
    ONIBUS("Passageiro de ônibus", 15, true),
    APP("Motorista de app", 22, true),
    PEDESTRE("Pedestre", 4.5, false),
    BIKE("Bicicleta", 14, false);

    private final String rotulo;
    private final double velocidadeMediaKmh;
    private final boolean motorizado;

    Modal(String rotulo, double velocidadeMediaKmh, boolean motorizado) {
        this.rotulo = rotulo;
        this.velocidadeMediaKmh = velocidadeMediaKmh;
        this.motorizado = motorizado;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Velocidade média em São Paulo, usada para estimar o tempo da rota. */
    public double getVelocidadeMediaKmh() {
        return velocidadeMediaKmh;
    }

    /** Veículos motorizados respeitam a mão única e podem usar vias expressas. */
    public boolean isMotorizado() {
        return motorizado;
    }
}
