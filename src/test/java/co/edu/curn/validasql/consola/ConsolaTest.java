package co.edu.curn.validasql.consola;

import co.edu.curn.validasql.servicio.EsquemaEjemplo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** La salida de la consola que muestra el documento de diseño (sección 8). */
class ConsolaTest {

    private static String ejecutar(String esquema, String consultas, boolean tokens, boolean arbol, boolean traza) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Consola.ejecutar(esquema, consultas, tokens, arbol, traza, new PrintStream(bytes, true, StandardCharsets.UTF_8));
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Imprime el esquema, la sentencia y sus diagnósticos como en el diseño")
    void salidaDelDiseno() {
        String salida = ejecutar(EsquemaEjemplo.esquema(), "SELECT nombr FROM estudiante WHERE semestre = 'cuarto';",
                false, false, false);

        assertTrue(salida.contains(
                "Esquema: 5 tablas cargadas (programa, estudiante, docente, asignatura, matricula). Sin errores."), salida);
        assertTrue(salida.contains("Sentencia 1 (línea 1): SELECT nombr FROM estudiante WHERE semestre = 'cuarto';"), salida);
        assertTrue(salida.contains(
                "[M02] Error semántico (línea 1, columna 8). La columna «nombr» no existe en la tabla «estudiante». ¿Quisiste decir «nombre»?"), salida);
        assertTrue(salida.contains(
                "[M05] Error semántico (línea 1, columna 36). No se puede comparar «semestre», que es INT, con una cadena de texto."), salida);
    }

    @Test
    @DisplayName("Las opciones muestran los tokens, el árbol y la traza LL(1)")
    void opcionesDeFases() {
        String salida = ejecutar(EsquemaEjemplo.esquema(), "SELECT nombre FROM estudiante WHERE semestre >= 4;",
                true, true, true);

        assertTrue(salida.contains("MAYORIG"), salida);
        assertTrue(salida.contains("└─ Comparación (>=)"), salida);
        assertTrue(salida.contains("Sent → Select ';'"), salida);
        assertTrue(salida.contains("Sentencia válida. Sin errores ni advertencias."), salida);
    }
}
