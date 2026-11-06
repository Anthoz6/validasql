package co.edu.curn.validasql.reporte;

import java.util.Comparator;

/**
 * Un error o una advertencia ubicado en el texto de entrada.
 *
 * @param codigo  código del catálogo (L01, S01, M02, A01...)
 * @param linea   línea del texto de entrada, contada desde 1
 * @param columna columna del primer carácter del elemento señalado, contada desde 1
 * @param mensaje texto explicativo ya formateado, sin el código ni la posición
 */
public record Diagnostico(CodigoDiagnostico codigo, int linea, int columna, String mensaje) {

    /** Ordena por línea y luego por columna; el orden es estable para posiciones iguales. */
    public static final Comparator<Diagnostico> POR_POSICION =
            Comparator.comparingInt(Diagnostico::linea).thenComparingInt(Diagnostico::columna);

    public Severidad severidad() {
        return codigo.severidad();
    }

    /** Devuelve una copia con la sugerencia «¿Quisiste decir ...?» al final del mensaje. */
    public Diagnostico conSugerencia(String nombreSugerido) {
        return new Diagnostico(codigo, linea, columna, mensaje + CodigoDiagnostico.sugerencia(nombreSugerido));
    }
}
