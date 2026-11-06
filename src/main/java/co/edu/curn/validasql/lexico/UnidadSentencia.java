package co.edu.curn.validasql.lexico;

import co.edu.curn.validasql.reporte.Diagnostico;

import java.util.List;

/**
 * Los tokens y errores léxicos de una sentencia, que el segmentador separa del resto del texto.
 *
 * @param numero          número de la sentencia dentro de su texto, desde 1
 * @param linea           línea donde empieza la sentencia
 * @param columna         columna donde empieza la sentencia
 * @param texto           texto de la sentencia tal como se escribió
 * @param tokens          tokens de la sentencia, incluido el {@code ;} final si lo tiene
 * @param erroresLexicos  errores L01 a L03 ocurridos dentro de la sentencia
 */
public record UnidadSentencia(int numero, int linea, int columna, String texto,
                              List<Token> tokens, List<Diagnostico> erroresLexicos) {

    public boolean tieneErroresLexicos() {
        return !erroresLexicos.isEmpty();
    }
}
