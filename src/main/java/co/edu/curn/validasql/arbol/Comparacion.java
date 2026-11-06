package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

/** {@code izquierdo operador derecho}, con alguno de los seis operadores relacionales. */
public record Comparacion(Operando izquierdo, Token operador, Operando derecho) implements Condicion {
}
