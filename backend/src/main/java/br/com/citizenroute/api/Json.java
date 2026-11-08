package br.com.citizenroute.api;

import br.com.citizenroute.exception.ValidacaoException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor e escritor de JSON feito só com Java puro, sem bibliotecas externas.
 *
 * <p>Conversões usadas:</p>
 * <ul>
 *   <li>objeto JSON ↔ Map&lt;String, Object&gt;</li>
 *   <li>array JSON ↔ List&lt;Object&gt;</li>
 *   <li>texto ↔ String; número ↔ Long ou Double; true/false ↔ Boolean; null ↔ null</li>
 *   <li>enum → nome da constante; LocalDateTime → texto ISO (2026-11-07T14:30:00)</li>
 * </ul>
 */
public final class Json {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private Json() {
    }

    // ---------------------------------------------------------------- escrita

    public static String escrever(Object valor) {
        StringBuilder saida = new StringBuilder();
        escrever(valor, saida);
        return saida.toString();
    }

    private static void escrever(Object valor, StringBuilder saida) {
        if (valor == null) {
            saida.append("null");
        } else if (valor instanceof String texto) {
            escreverTexto(texto, saida);
        } else if (valor instanceof Double numero && (numero.isNaN() || numero.isInfinite())) {
            saida.append("null");
        } else if (valor instanceof Number || valor instanceof Boolean) {
            saida.append(valor);
        } else if (valor instanceof Enum<?> constante) {
            escreverTexto(constante.name(), saida);
        } else if (valor instanceof LocalDateTime data) {
            escreverTexto(data.withNano(0).format(FORMATO_DATA), saida);
        } else if (valor instanceof Map<?, ?> mapa) {
            saida.append('{');
            boolean primeiro = true;
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                if (!primeiro) {
                    saida.append(',');
                }
                primeiro = false;
                escreverTexto(String.valueOf(entrada.getKey()), saida);
                saida.append(':');
                escrever(entrada.getValue(), saida);
            }
            saida.append('}');
        } else if (valor instanceof Collection<?> colecao) {
            saida.append('[');
            boolean primeiro = true;
            for (Object item : colecao) {
                if (!primeiro) {
                    saida.append(',');
                }
                primeiro = false;
                escrever(item, saida);
            }
            saida.append(']');
        } else {
            escreverTexto(valor.toString(), saida);
        }
    }

    private static void escreverTexto(String texto, StringBuilder saida) {
        saida.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> saida.append("\\\"");
                case '\\' -> saida.append("\\\\");
                case '\n' -> saida.append("\\n");
                case '\r' -> saida.append("\\r");
                case '\t' -> saida.append("\\t");
                case '\b' -> saida.append("\\b");
                case '\f' -> saida.append("\\f");
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

    // ---------------------------------------------------------------- leitura

    /** Lê um texto JSON. Lança ValidacaoException com a posição do erro se o JSON estiver malformado. */
    public static Object ler(String texto) {
        Leitor leitor = new Leitor(texto);
        leitor.pularEspacos();
        Object valor = leitor.lerValor();
        leitor.pularEspacos();
        if (!leitor.fim()) {
            throw leitor.erro("conteúdo extra depois do fim do JSON");
        }
        return valor;
    }

    /** Analisador recursivo: cada método lê um tipo de valor e avança a posição. */
    private static final class Leitor {
        private final String texto;
        private int posicao;

        Leitor(String texto) {
            this.texto = texto;
        }

        boolean fim() {
            return posicao >= texto.length();
        }

        void pularEspacos() {
            while (!fim() && Character.isWhitespace(texto.charAt(posicao))) {
                posicao++;
            }
        }

        Object lerValor() {
            if (fim()) {
                throw erro("o JSON terminou antes do esperado");
            }
            char c = texto.charAt(posicao);
            return switch (c) {
                case '{' -> lerObjeto();
                case '[' -> lerLista();
                case '"' -> lerTexto();
                case 't' -> lerPalavra("true", Boolean.TRUE);
                case 'f' -> lerPalavra("false", Boolean.FALSE);
                case 'n' -> lerPalavra("null", null);
                default -> {
                    if (c == '-' || Character.isDigit(c)) {
                        yield lerNumero();
                    }
                    throw erro("caractere inesperado '" + c + "'");
                }
            };
        }

        Map<String, Object> lerObjeto() {
            Map<String, Object> objeto = new LinkedHashMap<>();
            posicao++; // {
            pularEspacos();
            if (consumir('}')) {
                return objeto;
            }
            do {
                pularEspacos();
                if (fim() || texto.charAt(posicao) != '"') {
                    throw erro("esperava o nome de um campo entre aspas");
                }
                String chave = lerTexto();
                pularEspacos();
                if (!consumir(':')) {
                    throw erro("esperava ':' depois do nome do campo");
                }
                pularEspacos();
                objeto.put(chave, lerValor());
                pularEspacos();
            } while (consumir(','));
            if (!consumir('}')) {
                throw erro("esperava ',' ou '}'");
            }
            return objeto;
        }

        List<Object> lerLista() {
            List<Object> lista = new ArrayList<>();
            posicao++; // [
            pularEspacos();
            if (consumir(']')) {
                return lista;
            }
            do {
                pularEspacos();
                lista.add(lerValor());
                pularEspacos();
            } while (consumir(','));
            if (!consumir(']')) {
                throw erro("esperava ',' ou ']'");
            }
            return lista;
        }

        String lerTexto() {
            StringBuilder valor = new StringBuilder();
            posicao++; // aspas de abertura
            while (!fim()) {
                char c = texto.charAt(posicao++);
                if (c == '"') {
                    return valor.toString();
                }
                if (c != '\\') {
                    valor.append(c);
                    continue;
                }
                if (fim()) {
                    break;
                }
                char escape = texto.charAt(posicao++);
                switch (escape) {
                    case '"', '\\', '/' -> valor.append(escape);
                    case 'n' -> valor.append('\n');
                    case 'r' -> valor.append('\r');
                    case 't' -> valor.append('\t');
                    case 'b' -> valor.append('\b');
                    case 'f' -> valor.append('\f');
                    case 'u' -> {
                        if (posicao + 4 > texto.length()) {
                            throw erro("escape \\u incompleto");
                        }
                        try {
                            valor.append((char) Integer.parseInt(texto.substring(posicao, posicao + 4), 16));
                        } catch (NumberFormatException e) {
                            throw erro("escape \\u inválido");
                        }
                        posicao += 4;
                    }
                    default -> throw erro("escape inválido \\" + escape);
                }
            }
            throw erro("texto sem aspas de fechamento");
        }

        Number lerNumero() {
            int inicio = posicao;
            if (texto.charAt(posicao) == '-') {
                posicao++;
            }
            boolean decimal = false;
            while (!fim()) {
                char c = texto.charAt(posicao);
                if (Character.isDigit(c)) {
                    posicao++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    decimal = true;
                    posicao++;
                } else {
                    break;
                }
            }
            String numero = texto.substring(inicio, posicao);
            try {
                if (!decimal) {
                    try {
                        return Long.parseLong(numero);
                    } catch (NumberFormatException grande) {
                        return Double.parseDouble(numero);
                    }
                }
                return Double.parseDouble(numero);
            } catch (NumberFormatException e) {
                throw erro("número inválido '" + numero + "'");
            }
        }

        Object lerPalavra(String palavra, Object valor) {
            if (!texto.startsWith(palavra, posicao)) {
                throw erro("valor inválido");
            }
            posicao += palavra.length();
            return valor;
        }

        boolean consumir(char esperado) {
            if (!fim() && texto.charAt(posicao) == esperado) {
                posicao++;
                return true;
            }
            return false;
        }

        ValidacaoException erro(String motivo) {
            return new ValidacaoException("JSON inválido na posição " + posicao + ": " + motivo + ".");
        }
    }
}
