package co.edu.curn.validasql.semantico;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

/**
 * Propone el nombre correcto cuando alguien escribe mal una tabla o una columna.
 * Usa la distancia de Levenshtein: se sugiere el candidato más parecido si está a 2 ediciones o menos.
 */
public final class Sugeridor {

    /** Distancia máxima, en ediciones, para considerar que dos nombres se parecen. */
    static final int DISTANCIA_MAXIMA = 2;

    /**
     * @param escrito    lo que escribió el usuario
     * @param candidatos nombres que sí existen, en orden de preferencia
     * @return el candidato más cercano; si hay empate, el primero de la colección
     */
    public Optional<String> sugerir(String escrito, Collection<String> candidatos) {
        String buscado = escrito.toLowerCase(Locale.ROOT);
        String mejor = null;
        int mejorDistancia = DISTANCIA_MAXIMA + 1;
        for (String candidato : candidatos) {
            int distancia = distancia(buscado, candidato.toLowerCase(Locale.ROOT));
            if (distancia > 0 && distancia < mejorDistancia) {
                mejor = candidato;
                mejorDistancia = distancia;
            }
        }
        return Optional.ofNullable(mejor);
    }

    /**
     * Distancia de Levenshtein: el menor número de inserciones, borrados o sustituciones de un
     * carácter que convierten {@code a} en {@code b}. Programación dinámica con dos filas.
     */
    public static int distancia(String a, String b) {
        int[] anterior = new int[b.length() + 1];
        int[] actual = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            anterior[j] = j;                            // convertir "" en b[0..j] cuesta j inserciones
        }
        for (int i = 1; i <= a.length(); i++) {
            actual[0] = i;                              // convertir a[0..i] en "" cuesta i borrados
            for (int j = 1; j <= b.length(); j++) {
                int sustitucion = anterior[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                int borrado = anterior[j] + 1;
                int insercion = actual[j - 1] + 1;
                actual[j] = Math.min(sustitucion, Math.min(borrado, insercion));
            }
            int[] intercambio = anterior;
            anterior = actual;
            actual = intercambio;
        }
        return anterior[b.length()];
    }
}
