package br.com.citizenroute.service;

import br.com.citizenroute.model.AvaliavelRisco;
import br.com.citizenroute.model.Modal;
import br.com.citizenroute.model.NivelRisco;

import java.util.EnumMap;
import java.util.Map;

/**
 * Classifica o risco calculado pelo modelo e decide quando alertar.
 *
 * <p>Escala de 0 a 18 (gravidade 5 × peso 2.0 × fator 1.8):</p>
 * <ul>
 *   <li>abaixo de 4: baixo, sem alerta;</li>
 *   <li>de 4 a 6,9: moderado;</li>
 *   <li>de 7 a 9,9: alto;</li>
 *   <li>10 ou mais: crítico.</li>
 * </ul>
 */
public class RiscoService {

    public static final double LIMITE_MODERADO = 4.0;
    public static final double LIMITE_ALTO = 7.0;
    public static final double LIMITE_CRITICO = 10.0;

    /** A partir deste valor o cidadão recebe alerta. */
    public static final double LIMITE_ALERTA = LIMITE_MODERADO;

    public double calcular(AvaliavelRisco item, Modal modal) {
        return arredondar(item.calcularRisco(modal));
    }

    public NivelRisco classificar(double risco) {
        if (risco >= LIMITE_CRITICO) {
            return NivelRisco.CRITICO;
        }
        if (risco >= LIMITE_ALTO) {
            return NivelRisco.ALTO;
        }
        if (risco >= LIMITE_MODERADO) {
            return NivelRisco.MODERADO;
        }
        return NivelRisco.BAIXO;
    }

    public boolean deveAlertar(double risco) {
        return risco >= LIMITE_ALERTA;
    }

    /** Risco do mesmo item para cada modal; mostra na tela como cada pessoa é afetada. */
    public Map<Modal, Double> riscoPorModal(AvaliavelRisco item) {
        Map<Modal, Double> riscos = new EnumMap<>(Modal.class);
        for (Modal modal : Modal.values()) {
            riscos.put(modal, calcular(item, modal));
        }
        return riscos;
    }

    static double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
