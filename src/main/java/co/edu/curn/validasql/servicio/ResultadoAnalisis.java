package co.edu.curn.validasql.servicio;

import java.util.List;

/**
 * Resultado de analizar un esquema y unas consultas.
 *
 * @param nombresTablas nombres de las tablas que quedaron en la tabla de símbolos, en orden de declaración
 * @param esquema       resultado de cada sentencia del esquema
 * @param consultas     resultado de cada sentencia de las consultas
 */
public record ResultadoAnalisis(List<String> nombresTablas, List<ResultadoSentencia> esquema,
                                List<ResultadoSentencia> consultas) {

    /** Cuántas tablas quedaron en la tabla de símbolos. */
    public int tablas() {
        return nombresTablas.size();
    }

    /** Conteo de las consultas por estado. El esquema no entra en el conteo. */
    public record Resumen(int tablas, int sentencias, int validas, int conAdvertencias, int conErrores) {
    }

    public Resumen resumen() {
        return new Resumen(tablas(), consultas.size(),
                contar(ResultadoSentencia.Estado.VALIDA),
                contar(ResultadoSentencia.Estado.CON_ADVERTENCIAS),
                contar(ResultadoSentencia.Estado.CON_ERRORES));
    }

    private int contar(ResultadoSentencia.Estado estado) {
        return (int) consultas.stream().filter(s -> s.estado() == estado).count();
    }
}
