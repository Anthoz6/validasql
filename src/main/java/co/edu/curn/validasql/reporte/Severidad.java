package co.edu.curn.validasql.reporte;

/**
 * Tipo de un diagnóstico. La etiqueta es el texto que aparece en el reporte,
 * por ejemplo «Error semántico» en {@code [M02] Error semántico (línea 1, columna 8). ...}.
 */
public enum Severidad {
    ERROR_LEXICO("Error léxico"),
    ERROR_SINTACTICO("Error sintáctico"),
    ERROR_SEMANTICO("Error semántico"),
    ADVERTENCIA("Advertencia");

    private final String etiqueta;

    Severidad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Las advertencias no impiden aceptar la sentencia; todo lo demás sí. */
    public boolean esError() {
        return this != ADVERTENCIA;
    }
}
