package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

/**
 * Nombre de columna, con calificador o sin él ({@code tabla.columna}).
 *
 * @param calificador token de la tabla; {@code null} si la columna no está calificada
 * @param nombre      token de la columna
 */
public record ReferenciaColumna(Token calificador, Token nombre) implements Operando {

    @Override
    public Token primerToken() {
        return calificador != null ? calificador : nombre;
    }

    /** Texto tal como se escribió, por ejemplo {@code estudiante.nombre}. */
    public String textoOriginal() {
        return calificador != null ? calificador.lexema() + "." + nombre.lexema() : nombre.lexema();
    }
}
