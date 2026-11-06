package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

/**
 * {@code ORDER BY columna [ASC | DESC]}.
 *
 * @param direccion token ASC o DESC; {@code null} si no se escribió
 */
public record Orden(ReferenciaColumna columna, Token direccion) {
}
