package br.com.citizenroute.api;

/**
 * Status HTTP e corpo que serão devolvidos ao front-end.
 * O corpo é convertido para JSON pelo Roteador.
 */
public record Resposta(int status, Object corpo) {

    public static Resposta ok(Object corpo) {
        return new Resposta(200, corpo);
    }

    public static Resposta criado(Object corpo) {
        return new Resposta(201, corpo);
    }

    public static Resposta semConteudo() {
        return new Resposta(204, null);
    }
}
