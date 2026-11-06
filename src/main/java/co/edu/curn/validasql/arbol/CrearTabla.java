package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

import java.util.List;

/**
 * {@code CREATE TABLE nombre (columnas)}.
 *
 * @param nombre   token con el nombre de la tabla
 * @param columnas definiciones de columna, en el orden en que se declararon
 */
public record CrearTabla(Token nombre, List<DefColumna> columnas) implements Sentencia {
}
