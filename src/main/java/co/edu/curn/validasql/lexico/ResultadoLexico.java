package co.edu.curn.validasql.lexico;

import co.edu.curn.validasql.reporte.Diagnostico;

import java.util.List;

/**
 * Salida del análisis léxico de un texto completo.
 *
 * @param fuente  el texto analizado; el segmentador lo usa para recortar cada sentencia
 * @param tokens  los tokens en orden de aparición
 * @param errores los errores léxicos (L01 a L03) en orden de aparición
 */
public record ResultadoLexico(String fuente, List<Token> tokens, List<Diagnostico> errores) {
}
