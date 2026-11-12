package br.com.citizenlife.model;

/** Contrato de qualquer coisa cujo risco depende do modal de quem passa por ela. */
public interface AvaliavelRisco {

    /**
     * Calcula o risco bruto para um modal.
     * A escala vai de 0 a 18: gravidade (1 a 5) × peso do tipo (até 2.0) × fator do modal (até 1.8).
     */
    double calcularRisco(Modal modal);
}
