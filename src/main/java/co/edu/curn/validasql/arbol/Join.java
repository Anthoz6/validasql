package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

/** {@code INNER JOIN tabla ON condicion}. Solo se admite un JOIN por consulta. */
public record Join(Token tabla, Condicion condicion) {
}
