package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

/** {@code izquierda AND derecha} o {@code izquierda OR derecha}. Ambos asocian a la izquierda. */
public record Logica(Token operador, Condicion izquierda, Condicion derecha) implements Condicion {
}
