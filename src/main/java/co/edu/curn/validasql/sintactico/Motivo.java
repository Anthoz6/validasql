package co.edu.curn.validasql.sintactico;

/** Por qué el parser eligió una producción en un paso de la traza. */
public enum Motivo {
    /** El token actual está en PRIMERO de la producción. */
    PRIMERO,
    /** Se eligió ε porque el token actual está en SIGUIENTE del no terminal. */
    SIGUIENTE,
    /**
     * Se eligió ε pero el token actual no está en SIGUIENTE del no terminal: la sentencia tiene un
     * error, que el parser informa en el siguiente token que intenta consumir.
     */
    DESCARTE
}
