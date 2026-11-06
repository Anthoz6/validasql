package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

import java.util.List;

/**
 * {@code SELECT columnas FROM tabla [JOIN] [WHERE] [ORDER BY]}.
 * Las partes opcionales valen {@code null} cuando el parser eligió ε.
 *
 * @param asterisco token {@code *} si se pidieron todas las columnas; {@code null} si hay lista
 * @param columnas  columnas pedidas; vacía cuando hay asterisco
 * @param tabla     token con el nombre de la tabla del FROM
 */
public record Seleccion(Token asterisco, List<ReferenciaColumna> columnas, Token tabla,
                        Join join, Condicion where, Orden orden) implements Sentencia {
}
