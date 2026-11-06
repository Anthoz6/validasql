package co.edu.curn.validasql;

import co.edu.curn.validasql.servicio.EsquemaEjemplo;
import co.edu.curn.validasql.servicio.ResultadoAnalisis;
import co.edu.curn.validasql.servicio.ServicioAnalisis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.curn.validasql.Escenario.analizar;
import static co.edu.curn.validasql.Escenario.reporte;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Reglas semánticas que no cubren los ejemplos del anexo ni los casos C01 a C10. */
class AnalizadorSemanticoTest {

    /** Analiza contra el esquema ampliado de la universidad (cinco tablas). */
    private static ResultadoAnalisis universidad(String consultas) {
        return new ServicioAnalisis().analizar(EsquemaEjemplo.esquema(), consultas);
    }

    // ------------------------------------------------------------------ CREATE TABLE

    @Test
    @DisplayName("El esquema de ejemplo de la universidad carga las cinco tablas sin diagnósticos")
    void esquemaDeLaUniversidad() {
        ResultadoAnalisis r = universidad("");
        assertEquals(5, r.tablas());
        r.esquema().forEach(s -> assertEquals(List.of(), s.reportes()));
    }

    @Test
    @DisplayName("Un CREATE TABLE válido queda disponible para las sentencias siguientes")
    void createValidoSeRegistra() {
        ResultadoAnalisis r = analizar("CREATE TABLE curso (codigo INT PRIMARY KEY);\nSELECT codigo FROM curso;");
        assertEquals(List.of(), reporte(r, 1));
        assertEquals(List.of(), reporte(r, 2));
        assertEquals(3, r.tablas());
    }

    @Test
    @DisplayName("M08: columna repetida en CREATE TABLE, y la tabla no se registra")
    void columnaRepetidaEnCreate() {
        ResultadoAnalisis r = analizar("CREATE TABLE t (a INT, a INT);\nSELECT a FROM t;");
        assertEquals(List.of(
                "[M08] Error semántico (línea 1, columna 24). La columna «a» está repetida en la tabla «t»."),
                reporte(r, 1));
        assertEquals(List.of(
                "[M01] Error semántico (línea 2, columna 15). La tabla «t» no existe."),
                reporte(r, 2));
    }

    @Test
    @DisplayName("M08: dos claves primarias en CREATE TABLE")
    void dosClavesPrimarias() {
        assertEquals(List.of(
                "[M08] Error semántico (línea 1, columna 36). La tabla «t» ya tiene la clave primaria «a»; solo se admite una."),
                reporte(analizar("CREATE TABLE t (a INT PRIMARY KEY, b INT PRIMARY KEY);"), 1));
    }

    @Test
    @DisplayName("Mayúsculas y minúsculas son lo mismo en tablas, columnas y palabras reservadas")
    void sinDistinguirMayusculas() {
        ResultadoAnalisis r = analizar("select NOMBRE from ESTUDIANTE where Semestre >= 4;");
        assertEquals(List.of(), reporte(r, 1));
        assertEquals(List.of(
                "[M08] Error semántico (línea 1, columna 14). La tabla «PROGRAMA» ya existe."),
                reporte(analizar("create table PROGRAMA (id int);"), 1));
    }

    // ------------------------------------------------------------------ SELECT

    @Test
    @DisplayName("M02 en una columna calificada, con sugerencia")
    void columnaCalificadaInexistente() {
        assertEquals(List.of(
                "[M02] Error semántico (línea 1, columna 19). La columna «nombr» no existe en la tabla «estudiante». ¿Quisiste decir «nombre»?"),
                reporte(analizar("SELECT estudiante.nombr FROM estudiante;"), 1));
    }

    @Test
    @DisplayName("M02 en una consulta con JOIN se busca en las dos tablas")
    void columnaInexistenteConJoin() {
        assertEquals(List.of(
                "[M02] Error semántico (línea 1, columna 8). La columna «zzz» no existe en ninguna de las tablas de la consulta (estudiante, programa)."),
                reporte(analizar("SELECT zzz FROM estudiante INNER JOIN programa ON estudiante.id_programa = programa.id;"), 1));
    }

    @Test
    @DisplayName("Sin sugerencia cuando ningún nombre está a 2 ediciones o menos")
    void sinSugerenciaLejana() {
        assertEquals(List.of(
                "[M02] Error semántico (línea 1, columna 8). La columna «apellido» no existe en la tabla «estudiante»."),
                reporte(analizar("SELECT apellido FROM estudiante;"), 1));
    }

    @Test
    @DisplayName("Si la tabla no existe no se revisan sus columnas, ni siquiera calificadas")
    void tablaInexistenteNoEncadenaErrores() {
        assertEquals(List.of(
                "[M01] Error semántico (línea 1, columna 35). La tabla «estudiantes» no existe. ¿Quisiste decir «estudiante»?"),
                reporte(analizar("SELECT estudiantes.zzz, otra FROM estudiantes WHERE zzz = 1 ORDER BY otra;"), 1));
    }

    @Test
    @DisplayName("ORDER BY y el ON del JOIN también resuelven columnas")
    void orderByYOn() {
        assertEquals(List.of(
                "[M02] Error semántico (línea 1, columna 60). La columna «semestr» no existe en la tabla «estudiante». ¿Quisiste decir «semestre»?"),
                reporte(analizar("SELECT nombre FROM estudiante WHERE semestre >= 4 ORDER BY semestr DESC;"), 1));
    }

    @Test
    @DisplayName("M05 entre dos columnas de tipos distintos")
    void comparacionEntreColumnas() {
        assertEquals(List.of(
                "[M05] Error semántico (línea 1, columna 33). No se puede comparar «nombre», que es VARCHAR(60), con «semestre», que es INT."),
                reporte(analizar("SELECT id FROM estudiante WHERE nombre = semestre;"), 1));
    }

    @Test
    @DisplayName("INT y DECIMAL son compatibles: semestre >= 4.0 es válido")
    void numericosCompatibles() {
        assertEquals(List.of(), reporte(analizar("SELECT id FROM estudiante WHERE semestre >= 4.0;"), 1));
    }

    @Test
    @DisplayName("Dos literales de clases distintas no se pueden comparar (M05); NULL se compara con todo")
    void comparacionEntreLiterales() {
        assertEquals(List.of(
                "[M05] Error semántico (línea 1, columna 33). No se puede comparar un número con una cadena de texto."),
                reporte(analizar("SELECT id FROM estudiante WHERE 1 = 'a';"), 1));
        assertEquals(List.of(), reporte(analizar("SELECT id FROM estudiante WHERE 1 < 2;"), 1));
    }

    @Test
    @DisplayName("Comparar una columna DATE con un texto que no es fecha es M05, como en un INSERT")
    void dateEnComparaciones() {
        assertEquals(List.of(
                "[M05] Error semántico (línea 1, columna 32). El valor 'hoy' no es una fecha real con formato 'AAAA-MM-DD' para la columna «fecha», que es DATE."),
                reporte(universidad("SELECT id FROM matricula WHERE fecha = 'hoy';"), 1));
        assertEquals(List.of(), reporte(universidad("SELECT id FROM matricula WHERE fecha >= '2026-01-01';"), 1));
    }

    @Test
    @DisplayName("A02 también con <> y con NULL a la izquierda; IS NULL no avisa")
    void advertenciaDeNulos() {
        assertEquals(List.of(
                "[A02] Advertencia (línea 1, columna 42). Comparar con NULL usando «<>» nunca es verdadero. Usa IS NULL o IS NOT NULL."),
                reporte(analizar("SELECT id FROM estudiante WHERE semestre <> NULL;"), 1));
        assertEquals(List.of(), reporte(analizar("SELECT id FROM estudiante WHERE semestre IS NOT NULL;"), 1));
    }

    @Test
    @DisplayName("Una condición con AND, OR y paréntesis revisa todas sus comparaciones")
    void condicionCompuesta() {
        assertEquals(List.of(
                "[M02] Error semántico (línea 1, columna 44). La columna «sem» no existe en la tabla «estudiante».",
                "[M05] Error semántico (línea 1, columna 57). No se puede comparar «nombre», que es VARCHAR(60), con un número."),
                reporte(analizar("SELECT id FROM estudiante WHERE (id = 1 OR sem = 2) AND nombre = 3;"), 1));
    }

    // ------------------------------------------------------------------ INSERT

    @Test
    @DisplayName("INSERT sin lista de columnas y con todos los tipos válidos")
    void insertValidoSinLista() {
        assertEquals(List.of(), reporte(universidad("INSERT INTO docente VALUES (1, 'Ana', NULL, TRUE);"), 1));
        assertEquals(List.of(), reporte(universidad("INSERT INTO matricula VALUES (1, 2, 3, '2026-02-28', 4.5);"), 1));
    }

    @Test
    @DisplayName("M05 con una fecha que no existe en el calendario")
    void fechaInvalida() {
        assertEquals(List.of(
                "[M05] Error semántico (línea 1, columna 40). El valor '2026-02-30' no es una fecha real con formato 'AAAA-MM-DD' para la columna «fecha», que es DATE."),
                reporte(universidad("INSERT INTO matricula VALUES (1, 2, 3, '2026-02-30', 4.5);"), 1));
    }

    @Test
    @DisplayName("M05 al asignar una cadena a una columna BOOLEAN")
    void booleanConCadena() {
        assertEquals(List.of(
                "[M05] Error semántico (línea 1, columna 29). No se puede asignar una cadena de texto a la columna «activo», que es BOOLEAN."),
                reporte(universidad("UPDATE docente SET activo = 'si' WHERE id = 1;"), 1));
    }

    @Test
    @DisplayName("M07 al insertar NULL en una clave primaria")
    void nuloEnClavePrimaria() {
        assertEquals(List.of(
                "[M07] Error semántico (línea 1, columna 30). La columna «id» es NOT NULL y no puede recibir NULL."),
                reporte(analizar("INSERT INTO programa VALUES (NULL, 'Sistemas');"), 1));
    }

    @Test
    @DisplayName("M08 y M02 en la lista de columnas de un INSERT")
    void listaDeColumnasConProblemas() {
        assertEquals(List.of(
                "[M08] Error semántico (línea 1, columna 29). La columna «id» aparece más de una vez en la lista de columnas.",
                "[M02] Error semántico (línea 1, columna 33). La columna «nombr» no existe en la tabla «estudiante». ¿Quisiste decir «nombre»?"),
                reporte(analizar("INSERT INTO estudiante (id, id, nombr) VALUES (1, 2, 'x');"), 1));
    }

    @Test
    @DisplayName("Si las cantidades no coinciden solo se informa M06, no M07")
    void soloM06CuandoNoCoinciden() {
        assertEquals(List.of(
                "[M06] Error semántico (línea 1, columna 35). La sentencia nombra 1 columna pero entrega 2 valores."),
                reporte(analizar("INSERT INTO estudiante (semestre) VALUES (3, 4);"), 1));
    }

    // ------------------------------------------------------------------ UPDATE y DELETE

    @Test
    @DisplayName("A01 en un UPDATE sin WHERE")
    void updateSinWhere() {
        assertEquals(List.of(
                "[A01] Advertencia (línea 1, columna 1). La sentencia UPDATE no tiene WHERE, así que modificaría todos los registros de «estudiante»."),
                reporte(analizar("UPDATE estudiante SET semestre = 1;"), 1));
    }

    @Test
    @DisplayName("UPDATE con una columna inexistente y el tipo equivocado")
    void updateConErrores() {
        assertEquals(List.of(
                "[M02] Error semántico (línea 1, columna 23). La columna «semestr» no existe en la tabla «estudiante». ¿Quisiste decir «semestre»?",
                "[M05] Error semántico (línea 1, columna 47). No se puede asignar una cadena de texto a la columna «semestre», que es INT."),
                reporte(analizar("UPDATE estudiante SET semestr = 1, semestre = 'a' WHERE id = 1;"), 1));
    }

    @Test
    @DisplayName("DELETE con una tabla inexistente informa solo M01, aunque no tenga WHERE")
    void deleteConTablaInexistente() {
        assertEquals(List.of(
                "[M01] Error semántico (línea 1, columna 13). La tabla «estudiantes» no existe. ¿Quisiste decir «estudiante»?"),
                reporte(analizar("DELETE FROM estudiantes WHERE id = 1;"), 1));
    }

    // ------------------------------------------------------------------ varias sentencias

    @Test
    @DisplayName("Un error en una sentencia no impide analizar las demás")
    void sentenciasIndependientes() {
        ResultadoAnalisis r = analizar("SELECT # FROM estudiante;\nSELECT id FROM estudiante WHERE;\nSELECT id FROM estudiante;");
        assertEquals(3, r.consultas().size());
        assertEquals(1, reporte(r, 1).size());
        assertEquals(1, reporte(r, 2).size());
        assertEquals(List.of(), reporte(r, 3));
        assertEquals(1, r.resumen().validas());
        assertEquals(2, r.resumen().conErrores());
    }
}
