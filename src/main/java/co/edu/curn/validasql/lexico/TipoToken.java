package co.edu.curn.validasql.lexico;

/**
 * Los 48 tipos de token del anexo: 4 de valores, 6 operadores relacionales, 6 signos de puntuación
 * y 32 palabras reservadas.
 *
 * <p>El orden de declaración importa: el analizador sintáctico guarda los tokens esperados en un
 * {@link java.util.EnumSet}, que los recorre en este orden, y así el mensaje S01 sale con los
 * tokens en el mismo orden del anexo (por ejemplo «',', '.' o FROM»).</p>
 */
public enum TipoToken {

    // Valores
    IDENT("un identificador"),
    NUM_ENT("un número entero"),
    NUM_DEC("un número decimal"),
    CADENA("una cadena de texto"),

    // Operadores relacionales
    IGUAL("'='"),
    DISTINTO("'<>'"),
    MENOR("'<'"),
    MAYOR("'>'"),
    MENORIG("'<='"),
    MAYORIG("'>='"),

    // Puntuación
    COMA("','"),
    PUNTOCOMA("';'"),
    PAR_IZQ("'('"),
    PAR_DER("')'"),
    PUNTO("'.'"),
    ASTERISCO("'*'"),

    // Palabras reservadas: definición de tablas
    CREATE, TABLE, PRIMARY, KEY, NOT, NULL,
    // tipos de dato
    INT, DECIMAL, VARCHAR, DATE, BOOLEAN,
    // consulta
    SELECT, FROM, INNER, JOIN, ON, WHERE, ORDER, BY, ASC, DESC,
    // inserción
    INSERT, INTO, VALUES,
    // actualización y borrado
    UPDATE, SET, DELETE,
    // operadores lógicos
    AND, OR, IS,
    // literales lógicos
    TRUE, FALSE;

    private final String descripcion;

    /** Palabra reservada: se describe con su propio nombre. */
    TipoToken() {
        this.descripcion = name();
    }

    TipoToken(String descripcion) {
        this.descripcion = descripcion;
    }

    /** Cómo se nombra este token en un mensaje de «se esperaba ...». */
    public String descripcion() {
        return descripcion;
    }

    public boolean esPalabraReservada() {
        return ordinal() >= CREATE.ordinal();
    }
}
