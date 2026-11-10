package br.com.citizenroute.model;

import java.util.List;

/**
 * Resultado do cruzamento entre uma ocorrência ativa e um caminho: as rotas salvas do cidadão
 * ou uma rota que ele acabou de calcular no mapa.
 * Não é tabela: a AlertaService calcula os alertas a cada consulta.
 */
public class Alerta {

    private final Ocorrencia ocorrencia;
    private final List<Via> viasAfetadas;
    private final List<Rota> rotasAfetadas;
    private final double risco;
    private final NivelRisco nivel;
    private final boolean acimaDoLimite;

    public Alerta(Ocorrencia ocorrencia, List<Via> viasAfetadas, List<Rota> rotasAfetadas,
                  double risco, NivelRisco nivel, boolean acimaDoLimite) {
        this.ocorrencia = ocorrencia;
        this.viasAfetadas = List.copyOf(viasAfetadas);
        this.rotasAfetadas = List.copyOf(rotasAfetadas);
        this.risco = risco;
        this.nivel = nivel;
        this.acimaDoLimite = acimaDoLimite;
    }

    public Ocorrencia getOcorrencia() {
        return ocorrencia;
    }

    /** Vias do caminho atingidas pela ocorrência. */
    public List<Via> getViasAfetadas() {
        return viasAfetadas;
    }

    /** Rotas salvas que passam pelas vias atingidas (vazia quando a rota ainda não foi salva). */
    public List<Rota> getRotasAfetadas() {
        return rotasAfetadas;
    }

    public double getRisco() {
        return risco;
    }

    public NivelRisco getNivel() {
        return nivel;
    }

    /** Verdadeiro quando o risco passa do limite e o cidadão deve ser avisado. */
    public boolean isAcimaDoLimite() {
        return acimaDoLimite;
    }
}
