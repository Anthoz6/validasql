package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

/** {@code NOT condicion}. */
public record Negacion(Token not, Condicion condicion) implements Condicion {
}
