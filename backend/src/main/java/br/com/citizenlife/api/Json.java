package br.com.citizenlife.api;

import br.com.citizenlife.exception.ValidacaoException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor e escritor de JSON feito com Java puro, sem bibliotecas.
 * Objeto JSON ↔ Map, array ↔ List, texto ↔ String, número ↔ Long ou Double, true/false ↔ Boolean.
 */
public final class Json {

    private final String texto;
    private int posicao;

    private Json(String texto) {
        this.texto = texto;
    }

    // ------------------------------------------------------------------ escrita

    public static String escrever(Object valor) {
        StringBuilder saida = new StringBuilder();
        escrever(valor, saida);
        return saida.toString();
    }

    private static void escrever(Object valor, StringBuilder saida) {
        if (valor == null) {
            saida.append("null");
        } else if (valor instanceof Number || valor instanceof Boolean) {
            saida.append(valor);
        } else if (valor instanceof LocalDateTime data) {
            escreverTexto(data.withNano(0).toString(), saida);
        } else if (valor instanceof Map<?, ?> mapa) {
            saida.append('{');
            String separador = "";
            for (Map.Entry<?, ?> item : mapa.entrySet()) {
                saida.append(separador);
                escreverTexto(String.valueOf(item.getKey()), saida);
                saida.append(':');
                escrever(item.getValue(), saida);
                separador = ",";
            }
            saida.append('}');
        } else if (valor instanceof Collection<?> lista) {
            saida.append('[');
            String separador = "";
            for (Object item : lista) {
                saida.append(separador);
                escrever(item, saida);
                separador = ",";
            }
            saida.append(']');
        } else {
            escreverTexto(valor.toString(), saida);
        }
    }

    private static void escreverTexto(String valor, StringBuilder saida) {
        saida.append('"');
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"' -> saida.append("\\\"");
                case '\\' -> saida.append("\\\\");
                case '\n' -> saida.append("\\n");
                case '\r' -> saida.append("\\r");
                case '\t' -> saida.append("\\t");
                default -> {
                    if (c < 0x20) {
                        saida.append(String.format("\\u%04x", (int) c));
                    } else {
                        saida.append(c);
                    }
                }
            }
        }
        saida.append('"');
    }

    // ------------------------------------------------------------------ leitura

    /** Lê um texto JSON. Se estiver malformado, lança ValidacaoException com a posição do erro. */
    public static Object ler(String texto) {
        Json leitor = new Json(texto);
        Object valor = leitor.lerValor();
        leitor.pularEspacos();
        if (leitor.posicao < texto.length()) {
            throw leitor.erro("conteúdo extra depois do fim");
        }
        return valor;
    }

    private Object lerValor() {
        pularEspacos();
        if (posicao >= texto.length()) {
            throw erro("o texto terminou antes do esperado");
        }
        char c = texto.charAt(posicao);
        if (c == '{') return lerObjeto();
        if (c == '[') return lerLista();
        if (c == '"') return lerTexto();
        if (texto.startsWith("true", posicao)) return lerPalavra("true", Boolean.TRUE);
        if (texto.startsWith("false", posicao)) return lerPalavra("false", Boolean.FALSE);
        if (texto.startsWith("null", posicao)) return lerPalavra("null", null);
        if (c == '-' || Character.isDigit(c)) return lerNumero();
        throw erro("caractere inesperado '" + c + "'");
    }

    private Map<String, Object> lerObjeto() {
        Map<String, Object> objeto = new LinkedHashMap<>();
        posicao++;
        pularEspacos();
        if (consumir('}')) {
            return objeto;
        }
        do {
            pularEspacos();
            if (posicao >= texto.length() || texto.charAt(posicao) != '"') {
                throw erro("esperava o nome de um campo entre aspas");
            }
            String chave = lerTexto();
            pularEspacos();
            if (!consumir(':')) {
                throw erro("esperava ':' depois do nome do campo");
            }
            objeto.put(chave, lerValor());
            pularEspacos();
        } while (consumir(','));
        if (!consumir('}')) {
            throw erro("esperava ',' ou '}'");
        }
        return objeto;
    }

    private List<Object> lerLista() {
        List<Object> lista = new ArrayList<>();
        posicao++;
        pularEspacos();
        if (consumir(']')) {
            return lista;
        }
        do {
            lista.add(lerValor());
            pularEspacos();
        } while (consumir(','));
        if (!consumir(']')) {
            throw erro("esperava ',' ou ']'");
        }
        return lista;
    }

    private String lerTexto() {
        StringBuilder valor = new StringBuilder();
        posicao++;
        while (posicao < texto.length()) {
            char c = texto.charAt(posicao++);
            if (c == '"') {
                return valor.toString();
            }
            if (c != '\\' || posicao >= texto.length()) {
                valor.append(c);
                continue;
            }
            char escape = texto.charAt(posicao++);
            switch (escape) {
                case 'n' -> valor.append('\n');
                case 'r' -> valor.append('\r');
                case 't' -> valor.append('\t');
                case 'b' -> valor.append('\b');
                case 'f' -> valor.append('\f');
                case 'u' -> {
                    try {
                        valor.append((char) Integer.parseInt(texto.substring(posicao, posicao + 4), 16));
                    } catch (RuntimeException e) {
                        throw erro("escape \\u inválido");
                    }
                    posicao += 4;
                }
                default -> valor.append(escape);
            }
        }
        throw erro("texto sem aspas de fechamento");
    }

    private Number lerNumero() {
        int inicio = posicao++;
        while (posicao < texto.length() && "0123456789.eE+-".indexOf(texto.charAt(posicao)) >= 0) {
            posicao++;
        }
        String numero = texto.substring(inicio, posicao);
        try {
            return numero.matches("-?\\d{1,18}") ? Long.parseLong(numero) : Double.parseDouble(numero);
        } catch (NumberFormatException e) {
            throw erro("número inválido '" + numero + "'");
        }
    }

    private Object lerPalavra(String palavra, Object valor) {
        posicao += palavra.length();
        return valor;
    }

    private void pularEspacos() {
        while (posicao < texto.length() && Character.isWhitespace(texto.charAt(posicao))) {
            posicao++;
        }
    }

    private boolean consumir(char esperado) {
        if (posicao < texto.length() && texto.charAt(posicao) == esperado) {
            posicao++;
            return true;
        }
        return false;
    }

    private ValidacaoException erro(String motivo) {
        return new ValidacaoException("corpo", "JSON inválido na posição " + posicao + ": " + motivo + ".");
    }
}
