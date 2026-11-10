package br.com.citizenroute.api;

import br.com.citizenroute.exception.ValidacaoException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Leitura tipada do corpo JSON de uma requisição.
 * Campo ausente devolve nulo (a service decide se é obrigatório).
 * Campo com tipo errado gera ValidacaoException apontando o campo.
 */
public final class CorpoJson {

    private final Map<String, Object> dados;

    private CorpoJson(Map<String, Object> dados) {
        this.dados = dados;
    }

    @SuppressWarnings("unchecked")
    public static CorpoJson de(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ValidacaoException("O corpo da requisição está vazio. Envie os dados em JSON.");
        }
        Object valor = Json.ler(texto);
        if (!(valor instanceof Map<?, ?>)) {
            throw new ValidacaoException("O corpo da requisição precisa ser um objeto JSON, entre chaves.");
        }
        return new CorpoJson((Map<String, Object>) valor);
    }

    public String texto(String campo) {
        Object valor = dados.get(campo);
        if (valor == null) {
            return null;
        }
        if (valor instanceof String texto) {
            return texto;
        }
        if (valor instanceof Number || valor instanceof Boolean) {
            return valor.toString();
        }
        throw erro(campo, "deve ser um texto");
    }

    public Integer inteiro(String campo) {
        Long valor = longo(campo);
        if (valor == null) {
            return null;
        }
        if (valor > Integer.MAX_VALUE || valor < Integer.MIN_VALUE) {
            throw erro(campo, "está fora do intervalo permitido");
        }
        return valor.intValue();
    }

    /** Aceita número ou texto numérico, porque formulários HTML enviam valores de select como texto. */
    public Long longo(String campo) {
        Object valor = dados.get(campo);
        if (valor == null || (valor instanceof String texto && texto.isBlank())) {
            return null;
        }
        if (valor instanceof Long numero) {
            return numero;
        }
        if (valor instanceof Double numero && numero == Math.rint(numero)) {
            return numero.longValue();
        }
        if (valor instanceof String texto) {
            try {
                return Long.parseLong(texto.trim());
            } catch (NumberFormatException e) {
                throw erro(campo, "deve ser um número inteiro");
            }
        }
        throw erro(campo, "deve ser um número inteiro");
    }

    /** Número com casas decimais (coordenadas do mapa, por exemplo). */
    public Double decimal(String campo) {
        Object valor = dados.get(campo);
        if (valor == null || (valor instanceof String texto && texto.isBlank())) {
            return null;
        }
        if (valor instanceof Number numero) {
            return numero.doubleValue();
        }
        if (valor instanceof String texto) {
            try {
                return Double.parseDouble(texto.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                throw erro(campo, "deve ser um número");
            }
        }
        throw erro(campo, "deve ser um número");
    }

    /** Lê um objeto JSON aninhado, como {"origem": {"descricao": ..., "x": ..., "y": ...}}. Ausente vira nulo. */
    @SuppressWarnings("unchecked")
    public CorpoJson objeto(String campo) {
        Object valor = dados.get(campo);
        if (valor == null) {
            return null;
        }
        if (!(valor instanceof Map<?, ?>)) {
            throw erro(campo, "deve ser um objeto JSON");
        }
        return new CorpoJson((Map<String, Object>) valor);
    }

    /** Lê um array de números inteiros, como [3, 8, 12]. Campo ausente vira lista vazia. */
    public List<Long> listaDeNumeros(String campo) {
        Object valor = dados.get(campo);
        if (valor == null) {
            return List.of();
        }
        if (!(valor instanceof List<?> itens)) {
            throw erro(campo, "deve ser uma lista");
        }
        List<Long> resultado = new ArrayList<>();
        for (Object item : itens) {
            if (item instanceof Long numero) {
                resultado.add(numero);
            } else if (item instanceof Double numero && numero == Math.rint(numero)) {
                resultado.add(numero.longValue());
            } else {
                throw erro(campo, "deve conter apenas números inteiros");
            }
        }
        return resultado;
    }

    /** Converte o texto na constante do enum. Valor inválido lista as opções aceitas. */
    public <E extends Enum<E>> E opcao(String campo, Class<E> tipo) {
        String valor = texto(campo);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            String opcoes = Arrays.stream(tipo.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
            throw erro(campo, "tem um valor inválido. Use uma destas opções: " + opcoes);
        }
    }

    /** Lê um array de objetos JSON. Campo ausente vira lista vazia. */
    @SuppressWarnings("unchecked")
    public List<CorpoJson> lista(String campo) {
        Object valor = dados.get(campo);
        if (valor == null) {
            return List.of();
        }
        if (!(valor instanceof List<?> itens)) {
            throw erro(campo, "deve ser uma lista");
        }
        List<CorpoJson> resultado = new ArrayList<>();
        for (Object item : itens) {
            if (!(item instanceof Map<?, ?>)) {
                throw erro(campo, "deve conter objetos JSON");
            }
            resultado.add(new CorpoJson((Map<String, Object>) item));
        }
        return resultado;
    }

    private ValidacaoException erro(String campo, String problema) {
        return new ValidacaoException("O campo \"" + campo + "\" " + problema + ".",
                Map.of(campo, "Este campo " + problema + "."));
    }
}
