package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

/** {@code columna = literal} dentro del SET de un UPDATE. */
public record Asignacion(Token columna, Literal valor) {
}
