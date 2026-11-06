package co.edu.curn.validasql.lexico;

/**
 * Un token con el texto exacto que se leyó y su posición.
 *
 * @param tipo    tipo de token
 * @param lexema  texto tal como se escribió (una CADENA conserva sus comillas)
 * @param linea   línea del primer carácter, contada desde 1
 * @param columna columna del primer carácter, contada desde 1
 */
public record Token(TipoToken tipo, String lexema, int linea, int columna) {

    /** Columna del carácter siguiente al último del token. Sirve para ubicar «fin de la sentencia». */
    public int columnaFinal() {
        return columna + lexema.length();
    }
}
