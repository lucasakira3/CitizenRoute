package br.com.citizenlife.model;

/**
 * Saída ou destino de uma rota: o texto que o cidadão escolheu ("Rua Augusta", "Tatuapé")
 * e a posição no plano do mapa (x para o leste e y para o sul, em unidades de 25 m).
 * Com a posição, o front-end recalcula e desenha o caminho quando a rota salva é aberta.
 */
public record PontoRota(String descricao, double x, double y) {
}
