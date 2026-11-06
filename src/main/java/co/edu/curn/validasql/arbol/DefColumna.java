package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

/**
 * Definición de una columna en CREATE TABLE.
 *
 * @param nombre        token con el nombre de la columna
 * @param tipo          tipo de dato
 * @param longitud      n de {@code VARCHAR(n)}; 0 para los demás tipos
 * @param notNull       si se escribió NOT NULL (PRIMARY KEY lo implica, pero eso lo resuelve el semántico)
 * @param clavePrimaria si se escribió PRIMARY KEY
 */
public record DefColumna(Token nombre, TipoDato tipo, int longitud, boolean notNull, boolean clavePrimaria) {
}
