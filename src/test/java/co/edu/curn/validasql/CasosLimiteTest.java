package co.edu.curn.validasql;

import co.edu.curn.validasql.servicio.EsquemaEjemplo;
import co.edu.curn.validasql.servicio.ResultadoAnalisis;
import co.edu.curn.validasql.servicio.ServicioAnalisis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.curn.validasql.Escenario.analizar;
import static co.edu.curn.validasql.Escenario.reporte;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Casos límite de la clase 5: valores justo en el borde de una regla, entradas vacías o raras y
 * textos que podrían romper al analizador. Ninguno debe lanzar una excepción.
 */
class CasosLimiteTest {

    private static ResultadoAnalisis universidad(String consultas) {
        return new ServicioAnalisis().analizar(EsquemaEjemplo.esquema(), consultas);
    }

    // ------------------------------------------------------------------ bordes de las reglas

    @Test
    @DisplayName("VARCHAR(60): un texto de exactamente 60 caracteres es válido y uno de 61 es M05")
    void bordeDeLongitud() {
        assertEquals(List.of(), reporte(analizar("INSERT INTO programa VALUES (3, '" + "x".repeat(60) + "');"), 1));
        assertEquals(List.of(
                "[M05] Error semántico (línea 1, columna 33). El texto tiene 61 caracteres y la columna «nombre» admite como máximo 60."),
                reporte(analizar("INSERT INTO programa VALUES (3, '" + "x".repeat(61) + "');"), 1));
    }

    @Test
    @DisplayName("Un texto con tildes y ñ cuenta cada letra como un carácter")
    void tildesCuentanComoUnCaracter() {
        assertEquals(List.of(), reporte(analizar("INSERT INTO programa VALUES (3, '" + "ñ".repeat(60) + "');"), 1));
    }

    @Test
    @DisplayName("DATE: año bisiesto válido, 29 de febrero inexistente y formato incompleto")
    void bordesDeFecha() {
        assertEquals(List.of(), reporte(universidad("INSERT INTO matricula VALUES (1, 1, 1, '2028-02-29', 4.5);"), 1));
        assertEquals(1, reporte(universidad("INSERT INTO matricula VALUES (1, 1, 1, '2026-02-29', 4.5);"), 1).size());
        assertEquals(1, reporte(universidad("INSERT INTO matricula VALUES (1, 1, 1, '2026-2-9', 4.5);"), 1).size());
        assertEquals(1, reporte(universidad("INSERT INTO matricula VALUES (1, 1, 1, '2026-13-01', 4.5);"), 1).size());
    }

    @Test
    @DisplayName("Sugerencia: a 2 ediciones sí, a 3 no")
    void bordeDeSugerencia() {
        assertEquals(List.of(
                "[M02] Error semántico (línea 1, columna 8). La columna «nmbr» no existe en la tabla «estudiante». ¿Quisiste decir «nombre»?"),
                reporte(analizar("SELECT nmbr FROM estudiante;"), 1));
        assertEquals(List.of(
                "[M02] Error semántico (línea 1, columna 8). La columna «nmb» no existe en la tabla «estudiante»."),
                reporte(analizar("SELECT nmb FROM estudiante;"), 1));
    }

    @Test
    @DisplayName("Un literal numérico entero cabe en una columna DECIMAL y uno decimal en una INT")
    void numericosEnElBorde() {
        assertEquals(List.of(), reporte(universidad("INSERT INTO matricula VALUES (1, 1, 1, '2026-01-01', 5);"), 1));
        assertEquals(List.of(), reporte(universidad("UPDATE estudiante SET semestre = 4.5 WHERE id = 1;"), 1));
    }

    // ------------------------------------------------------------------ entradas vacías o raras

    @Test
    @DisplayName("Esquema y consultas vacíos: cero tablas, cero sentencias")
    void entradasVacias() {
        ResultadoAnalisis r = new ServicioAnalisis().analizar("", "");
        assertEquals(0, r.tablas());
        assertEquals(0, r.resumen().sentencias());
        assertEquals(0, new ServicioAnalisis().analizar(null, null).resumen().sentencias());
    }

    @Test
    @DisplayName("Sin esquema, toda tabla es inexistente (M01) y no se encadenan errores de columnas")
    void sinEsquema() {
        ResultadoAnalisis r = new ServicioAnalisis().analizar("", "SELECT nombre FROM estudiante WHERE semestre = 'a';");
        assertEquals(List.of(
                "[M01] Error semántico (línea 1, columna 20). La tabla «estudiante» no existe."),
                r.consultas().get(0).reportes());
    }

    @Test
    @DisplayName("Una sentencia sin punto y coma final es S01 al final de la sentencia")
    void sinPuntoYComaFinal() {
        assertEquals(List.of(
                "[S01] Error sintáctico (línea 1, columna 30). Se esperaba ';', INNER, WHERE u ORDER, pero se encontró el final de la sentencia."),
                reporte(analizar("SELECT nombre FROM estudiante"), 1));
    }

    @Test
    @DisplayName("Un punto y coma suelto es una sentencia vacía y da S01")
    void puntoYComaSuelto() {
        assertEquals(List.of(
                "[S01] Error sintáctico (línea 1, columna 1). Se esperaba CREATE, SELECT, INSERT, UPDATE o DELETE, pero se encontró el símbolo «;»."),
                reporte(analizar(";"), 1));
    }

    @Test
    @DisplayName("Saltos de línea de Windows (CRLF) y tabulaciones no desplazan las columnas")
    void crlfYTabulaciones() {
        ResultadoAnalisis r = analizar("SELECT zzz FROM estudiante;\r\n\tSELECT yyy FROM estudiante;");
        assertEquals(List.of("[M02] Error semántico (línea 1, columna 8). La columna «zzz» no existe en la tabla «estudiante»."),
                reporte(r, 1));
        assertEquals(List.of("[M02] Error semántico (línea 2, columna 9). La columna «yyy» no existe en la tabla «estudiante»."),
                reporte(r, 2));
        assertEquals(2, r.consultas().get(1).linea());
        assertEquals(2, r.consultas().get(1).columna());
    }

    @Test
    @DisplayName("Palabras reservadas mezclando mayúsculas y minúsculas")
    void mayusculasMezcladas() {
        assertEquals(List.of(), reporte(analizar("SeLeCt NoMbRe FrOm EsTuDiAnTe WhErE sEmEsTrE >= 4;"), 1));
    }

    @Test
    @DisplayName("Un emoji dentro de una cadena es válido; fuera de ella es un solo L01")
    void emoji() {
        assertEquals(List.of(), reporte(analizar("INSERT INTO programa VALUES (3, 'Sistemas 😀');"), 1));
        List<String> fuera = reporte(analizar("SELECT 😀 FROM estudiante;"), 1);
        assertEquals(1, fuera.size());
        assertTrue(fuera.get(0).startsWith("[L01]"), fuera.get(0));
    }

    @Test
    @DisplayName("Varios errores léxicos en una sentencia se reportan todos")
    void variosErroresLexicos() {
        assertEquals(3, reporte(analizar("SELECT # nombre @ FROM estudiante WHERE semestre = 3.;"), 1).size());
    }

    // ------------------------------------------------------------------ robustez

    @Test
    @DisplayName("Un VARCHAR con una longitud gigante no rompe el analizador")
    void longitudGigante() {
        ResultadoAnalisis r = new ServicioAnalisis().analizar("CREATE TABLE t (a VARCHAR(99999999999999999999));", "");
        assertEquals(List.of(), r.esquema().get(0).reportes());
    }

    @Test
    @DisplayName("Doscientos niveles de paréntesis anidados se analizan sin desbordar la pila")
    void parentesisProfundos() {
        String condicion = "(".repeat(200) + "id = 1" + ")".repeat(200);
        assertEquals(List.of(), reporte(analizar("SELECT id FROM estudiante WHERE " + condicion + ";"), 1));
    }

    @Test
    @DisplayName("Una condición con miles de AND no desborda la pila")
    void condicionLarga() {
        String condicion = "id = 1" + " AND id = 1".repeat(3000);
        assertDoesNotThrow(() -> analizar("SELECT id FROM estudiante WHERE " + condicion + ";"));
    }

    @Test
    @DisplayName("Mil sentencias se analizan y cada una conserva su número y su línea")
    void muchasSentencias() {
        String consultas = "SELECT id FROM estudiante;\n".repeat(1000);
        ResultadoAnalisis r = analizar(consultas);
        assertEquals(1000, r.consultas().size());
        assertEquals(1000, r.consultas().get(999).numero());
        assertEquals(1000, r.consultas().get(999).linea());
        assertEquals(1000, r.resumen().validas());
    }

    @Test
    @DisplayName("Un mismo servicio analiza varias veces sin arrastrar tablas de un análisis a otro")
    void sinEstadoCompartido() {
        ServicioAnalisis servicio = new ServicioAnalisis();
        assertEquals(1, servicio.analizar("CREATE TABLE alpha (x INT);", "").tablas());
        assertEquals(1, servicio.analizar("CREATE TABLE zeta_larga (y INT);", "").tablas());
        assertEquals(List.of("[M01] Error semántico (línea 1, columna 15). La tabla «alpha» no existe."),
                servicio.analizar("CREATE TABLE zeta_larga (y INT);", "SELECT x FROM alpha;").consultas().get(0).reportes());
    }
}
