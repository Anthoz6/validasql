package co.edu.curn.validasql.sintactico;

/**
 * Una decisión del parser LL(1): con el token actual, qué producción eligió para un no terminal.
 *
 * @param paso        número de la decisión, desde 1
 * @param noTerminal  no terminal que se expandió, por ejemplo {@code ColQ}
 * @param token       lexema del token actual; {@code (fin)} si ya no quedaban tokens
 * @param produccion  producción elegida, por ejemplo {@code ColQ → ε}
 * @param motivo      por qué se eligió
 */
public record PasoTraza(int paso, String noTerminal, String token, String produccion, Motivo motivo) {
}
