package co.edu.curn.validasql;

import co.edu.curn.validasql.servicio.ResultadoAnalisis;
import co.edu.curn.validasql.servicio.ResultadoSentencia;
import co.edu.curn.validasql.servicio.ResultadoSentencia.Estado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.curn.validasql.Escenario.analizar;
import static co.edu.curn.validasql.Escenario.reporte;
import static co.edu.curn.validasql.Escenario.sentencia;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Los 8 ejemplos del anexo técnico de la clase 2, con su salida exacta: código, línea, columna y
 * mensaje. Es la retroalimentación de la docente: «los mismos ejemplos tienen que funcionar con el
 * programa». Si cambia una palabra de un mensaje, estas pruebas fallan.
 */
class EjemplosAnexoTest {

    @Test
    @DisplayName("Esquema común: 2 tablas cargadas, sin errores ni advertencias")
    void esquemaComun() {
        ResultadoAnalisis resultado = analizar("");

        assertEquals(2, resultado.tablas());
        assertEquals(2, resultado.esquema().size());
        resultado.esquema().forEach(s -> assertEquals(List.of(), s.reportes()));
    }

    @Test
    @DisplayName("Ejemplo 1: consulta válida, 9 tokens, árbol del anexo y 22 decisiones LL(1)")
    void ejemplo1ConsultaValida() {
        ResultadoAnalisis resultado = analizar("SELECT nombre FROM estudiante WHERE semestre >= 4;");
        ResultadoSentencia s = sentencia(resultado, 1);

        assertEquals(Estado.VALIDA, s.estado());
        assertEquals(List.of(), s.reportes());
        assertEquals(9, s.tokens().size());
        assertEquals("""
                Select
                ├─ columnas: Col(nombre)
                ├─ FROM: estudiante
                └─ WHERE
                   └─ Comparación (>=)
                      ├─ Col(semestre)
                      └─ NUM_ENT(4)""", s.arbolTexto());
        assertEquals(22, s.traza().size());
    }

    @Test
    @DisplayName("Ejemplo 1: la tabla de tokens (tipo, lexema, línea y columna)")
    void ejemplo1Tokens() {
        ResultadoSentencia s = sentencia(analizar("SELECT nombre FROM estudiante WHERE semestre >= 4;"), 1);

        List<String> esperados = List.of(
                "SELECT SELECT 1 1", "IDENT nombre 1 8", "FROM FROM 1 15", "IDENT estudiante 1 20",
                "WHERE WHERE 1 31", "IDENT semestre 1 37", "MAYORIG >= 1 46", "NUM_ENT 4 1 49",
                "PUNTOCOMA ; 1 50");
        List<String> obtenidos = s.tokens().stream()
                .map(t -> t.tipo() + " " + t.lexema() + " " + t.linea() + " " + t.columna())
                .toList();
        assertEquals(esperados, obtenidos);
    }

    @Test
    @DisplayName("Ejemplo 2: JOIN con columnas calificadas e INSERT, las dos válidas")
    void ejemplo2JoinEInsercion() {
        ResultadoAnalisis resultado = analizar("""
                SELECT estudiante.nombre, programa.nombre
                FROM estudiante INNER JOIN programa ON estudiante.id_programa = programa.id
                WHERE estudiante.semestre >= 4 ORDER BY estudiante.nombre DESC;

                INSERT INTO estudiante (id, nombre) VALUES (1, 'Ana');""");

        assertEquals(2, resultado.consultas().size());
        assertEquals(List.of(), reporte(resultado, 1));
        assertEquals(List.of(), reporte(resultado, 2));
        assertEquals(Estado.VALIDA, sentencia(resultado, 1).estado());
        assertEquals(Estado.VALIDA, sentencia(resultado, 2).estado());
    }

    @Test
    @DisplayName("Ejemplo 3: L01 (1, 46)")
    void ejemplo3ErrorLexicoL01() {
        ResultadoAnalisis resultado = analizar("SELECT nombre FROM estudiante WHERE semestre # 4;");

        assertEquals(List.of(
                "[L01] Error léxico (línea 1, columna 46). Carácter no válido «#»: no pertenece al alfabeto del lenguaje."),
                reporte(resultado, 1));
        assertNull(sentencia(resultado, 1).arbol(), "con errores léxicos la sentencia no pasa al parser");
    }

    @Test
    @DisplayName("Ejemplo 3: L02 (1, 46)")
    void ejemplo3ErrorLexicoL02() {
        ResultadoAnalisis resultado = analizar("SELECT nombre FROM estudiante WHERE nombre = 'Ana;");

        assertEquals(List.of(
                "[L02] Error léxico (línea 1, columna 46). Cadena sin cerrar: falta la comilla simple de cierre antes del fin de la línea."),
                reporte(resultado, 1));
    }

    @Test
    @DisplayName("Ejemplo 3: L03 (1, 48)")
    void ejemplo3ErrorLexicoL03() {
        ResultadoAnalisis resultado = analizar("SELECT nombre FROM estudiante WHERE semestre = 3.;");

        assertEquals(List.of(
                "[L03] Error léxico (línea 1, columna 48). Número decimal incompleto: después del punto se esperaba un dígito."),
                reporte(resultado, 1));
    }

    @Test
    @DisplayName("Ejemplo 4: S01 (1, 15) con ',', '.' o FROM, y S01 (2, 8) con FROM")
    void ejemplo4ErroresSintacticos() {
        ResultadoAnalisis resultado = analizar("SELECT nombre estudiante;\nDELETE estudiante;");

        assertEquals(2, resultado.consultas().size());
        assertEquals(List.of(
                "[S01] Error sintáctico (línea 1, columna 15). Se esperaba ',', '.' o FROM, pero se encontró el identificador «estudiante»."),
                reporte(resultado, 1));
        assertEquals(List.of(
                "[S01] Error sintáctico (línea 2, columna 8). Se esperaba FROM, pero se encontró el identificador «estudiante»."),
                reporte(resultado, 2));
        assertNull(sentencia(resultado, 1).arbol());
    }

    @Test
    @DisplayName("Ejemplo 5: M02 con sugerencia «nombre» (1, 8) y M05 (1, 36), con 22 decisiones LL(1)")
    void ejemplo5ColumnaYTipos() {
        ResultadoAnalisis resultado = analizar("SELECT nombr FROM estudiante WHERE semestre = 'cuarto';");

        assertEquals(List.of(
                "[M02] Error semántico (línea 1, columna 8). La columna «nombr» no existe en la tabla «estudiante». ¿Quisiste decir «nombre»?",
                "[M05] Error semántico (línea 1, columna 36). No se puede comparar «semestre», que es INT, con una cadena de texto."),
                reporte(resultado, 1));
        assertNotNull(sentencia(resultado, 1).arbol());
        assertEquals(22, sentencia(resultado, 1).traza().size());
        assertEquals(9, sentencia(resultado, 1).tokens().size());
        assertEquals(47, sentencia(resultado, 1).tokens().get(7).columna(), "'cuarto' es un CADENA en la columna 47");
    }

    @Test
    @DisplayName("Ejemplo 6: M03 columna ambigua (1, 8)")
    void ejemplo6ColumnaAmbigua() {
        ResultadoAnalisis resultado = analizar(
                "SELECT id FROM estudiante INNER JOIN programa ON estudiante.id_programa = programa.id;");

        assertEquals(List.of(
                "[M03] Error semántico (línea 1, columna 8). La columna «id» es ambigua: existe en «estudiante» y «programa». Califícala, por ejemplo «estudiante.id»."),
                reporte(resultado, 1));
    }

    @Test
    @DisplayName("Ejemplo 7: M06 cantidad de valores (1, 37)")
    void ejemplo7CantidadDeValores() {
        ResultadoAnalisis resultado = analizar("INSERT INTO estudiante (id, nombre) VALUES (1);");

        assertEquals(List.of(
                "[M06] Error semántico (línea 1, columna 37). La sentencia nombra 2 columnas pero entrega 1 valor."),
                reporte(resultado, 1));
    }

    @Test
    @DisplayName("Ejemplo 8: A01 (1, 1), A03 (2, 8) y A02 (2, 41); son advertencias y la sentencia se acepta")
    void ejemplo8Advertencias() {
        ResultadoAnalisis resultado = analizar("DELETE FROM estudiante;\nSELECT * FROM estudiante WHERE semestre = NULL;");

        assertEquals(List.of(
                "[A01] Advertencia (línea 1, columna 1). La sentencia DELETE no tiene WHERE, así que eliminaría todos los registros de «estudiante»."),
                reporte(resultado, 1));
        assertEquals(List.of(
                "[A03] Advertencia (línea 2, columna 8). SELECT * devuelve todas las columnas; conviene listar solo las necesarias.",
                "[A02] Advertencia (línea 2, columna 41). Comparar con NULL usando «=» nunca es verdadero. Usa IS NULL o IS NOT NULL."),
                reporte(resultado, 2));
        assertEquals(Estado.CON_ADVERTENCIAS, sentencia(resultado, 1).estado());
        assertEquals(Estado.CON_ADVERTENCIAS, sentencia(resultado, 2).estado());
        assertNotNull(sentencia(resultado, 2).arbol());
    }
}
