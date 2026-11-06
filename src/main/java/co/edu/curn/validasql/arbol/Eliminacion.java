package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

/**
 * {@code DELETE FROM tabla [WHERE condicion]}.
 *
 * @param delete token DELETE, donde se informa A01
 * @param where  condición; {@code null} si no hay WHERE
 */
public record Eliminacion(Token delete, Token tabla, Condicion where) implements Sentencia {
}
