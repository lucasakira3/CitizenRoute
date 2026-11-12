package br.com.citizenlife.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Problema numa via, como um alagamento ou um acidente (tabela CL_OCORRENCIA).
 *
 * <p>Aqui fica a regra central do projeto: o risco depende de como a pessoa se locomove.</p>
 * <pre>risco = gravidade (1 a 5) × peso do tipo × fator do modal</pre>
 * <p>O mesmo alagamento de gravidade 3 vale 3,6 para quem está de carro e 8,1 para quem está de moto.</p>
 */
public class Ocorrencia {

    /** Meios de transporte, na ordem das colunas de {@link #FATORES}. */
    public static final List<String> MODAIS = List.of("CARRO", "MOTO", "ONIBUS", "APP", "PEDESTRE", "BIKE");

    /** Peso de cada tipo de ocorrência. */
    private static final Map<String, Double> PESOS = Map.of(
            "ACIDENTE", 1.5, "ALAGAMENTO", 1.5, "QUEDA_ARVORE", 1.3, "SEMAFORO_APAGADO", 1.0,
            "DESLIZAMENTO", 2.0, "VIA_BLOQUEADA", 1.0, "INSEGURANCA", 1.2);

    /** Fator de exposição de cada modal (CARRO, MOTO, ONIBUS, APP, PEDESTRE, BIKE) a cada tipo. */
    private static final Map<String, double[]> FATORES = Map.of(
            "ACIDENTE",         new double[]{1.0, 1.6, 0.8, 1.0, 1.2, 1.5},
            "ALAGAMENTO",       new double[]{0.8, 1.8, 0.6, 0.8, 1.8, 1.6},
            "QUEDA_ARVORE",     new double[]{1.0, 1.5, 0.8, 1.0, 1.4, 1.5},
            "SEMAFORO_APAGADO", new double[]{1.0, 1.3, 0.8, 1.0, 1.6, 1.4},
            "DESLIZAMENTO",     new double[]{1.2, 1.6, 1.0, 1.2, 1.8, 1.6},
            "VIA_BLOQUEADA",    new double[]{0.8, 0.7, 1.0, 0.9, 0.5, 0.6},
            "INSEGURANCA",      new double[]{0.6, 1.2, 1.5, 1.3, 1.8, 1.4});

    /** A partir deste risco, o cidadão recebe alerta. */
    public static final double LIMITE_ALERTA = 4.0;

    private Long id;
    private String titulo;
    private String descricao;
    private String tipo;
    private int gravidade;
    private LocalDateTime dataRegistro;
    private Via via;

    /** Risco para o modal, com uma casa decimal. */
    public double calcularRisco(String modal) {
        double fator = FATORES.get(tipo)[MODAIS.indexOf(modal)];
        return Math.round(gravidade * PESOS.get(tipo) * fator * 10) / 10.0;
    }

    /** Nível do risco: BAIXO (abaixo de 4), MODERADO, ALTO (a partir de 7) ou CRITICO (a partir de 10). */
    public static String nivel(double risco) {
        if (risco >= 10) return "CRITICO";
        if (risco >= 7) return "ALTO";
        if (risco >= LIMITE_ALERTA) return "MODERADO";
        return "BAIXO";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getGravidade() {
        return gravidade;
    }

    public void setGravidade(int gravidade) {
        this.gravidade = gravidade;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDateTime dataRegistro) {
        this.dataRegistro = dataRegistro;
    }

    public Via getVia() {
        return via;
    }

    public void setVia(Via via) {
        this.via = via;
    }
}
