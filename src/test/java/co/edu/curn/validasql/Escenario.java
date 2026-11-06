package co.edu.curn.validasql;

import co.edu.curn.validasql.servicio.ResultadoAnalisis;
import co.edu.curn.validasql.servicio.ResultadoSentencia;
import co.edu.curn.validasql.servicio.ServicioAnalisis;

import java.util.List;

/**
 * Ayuda común de las pruebas: el esquema común del anexo (sección 8) y atajos para analizar
 * consultas y obtener el reporte con el formato exacto.
 */
final class Escenario {

    /** Esquema común del anexo: lo usan todos los ejemplos y casos. */
    static final String ESQUEMA_COMUN = """
            CREATE TABLE programa (id INT PRIMARY KEY, nombre VARCHAR(60) NOT NULL);
            CREATE TABLE estudiante (id INT PRIMARY KEY, nombre VARCHAR(60) NOT NULL,
                                     semestre INT, id_programa INT);""";

    private static final ServicioAnalisis SERVICIO = new ServicioAnalisis();

    private Escenario() {
    }

    static ResultadoAnalisis analizar(String consultas) {
        return SERVICIO.analizar(ESQUEMA_COMUN, consultas);
    }

    /** Reporte de la sentencia número {@code numero} (desde 1) de las consultas. */
    static List<String> reporte(ResultadoAnalisis resultado, int numero) {
        return resultado.consultas().get(numero - 1).reportes();
    }

    static ResultadoSentencia sentencia(ResultadoAnalisis resultado, int numero) {
        return resultado.consultas().get(numero - 1);
    }
}
