package co.edu.curn.validasql.semantico;

import co.edu.curn.validasql.arbol.TipoDato;

/**
 * Columna registrada en la tabla de símbolos.
 *
 * @param nombre        nombre en minúscula
 * @param tipo          tipo de dato
 * @param longitud      n de {@code VARCHAR(n)}; 0 para los demás tipos
 * @param notNull       true si es NOT NULL o clave primaria (PRIMARY KEY implica NOT NULL)
 * @param clavePrimaria true si es la clave primaria de la tabla
 */
public record Columna(String nombre, TipoDato tipo, int longitud, boolean notNull, boolean clavePrimaria) {

    /** Cómo se escribe el tipo en un mensaje: {@code INT} o {@code VARCHAR(60)}. */
    public String tipoCompleto() {
        return tipo == TipoDato.VARCHAR ? "VARCHAR(" + longitud + ")" : tipo.name();
    }
}
