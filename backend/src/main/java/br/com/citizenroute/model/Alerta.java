package br.com.citizenroute.model;

import java.util.List;

/**
 * Resultado do cruzamento entre uma ocorrência ativa e as vias que o cidadão monitora.
 * Não é tabela: a AlertaService calcula os alertas a cada consulta.
 */
public class Alerta {

    private final Ocorrencia ocorrencia;
    private final List<ViaMonitorada> viasAfetadas;
    private final double risco;
    private final NivelRisco nivel;
    private final boolean acimaDoLimite;

    public Alerta(Ocorrencia ocorrencia, List<ViaMonitorada> viasAfetadas,
                  double risco, NivelRisco nivel, boolean acimaDoLimite) {
        this.ocorrencia = ocorrencia;
        this.viasAfetadas = List.copyOf(viasAfetadas);
        this.risco = risco;
        this.nivel = nivel;
        this.acimaDoLimite = acimaDoLimite;
    }

    public Ocorrencia getOcorrencia() {
        return ocorrencia;
    }

    public List<ViaMonitorada> getViasAfetadas() {
        return viasAfetadas;
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
