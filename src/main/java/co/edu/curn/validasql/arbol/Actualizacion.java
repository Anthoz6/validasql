package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

import java.util.List;

/**
 * {@code UPDATE tabla SET asignaciones [WHERE condicion]}.
 *
 * @param update token UPDATE, donde se informa A01
 * @param where  condición; {@code null} si no hay WHERE
 */
public record Actualizacion(Token update, Token tabla, List<Asignacion> asignaciones, Condicion where)
        implements Sentencia {
}
