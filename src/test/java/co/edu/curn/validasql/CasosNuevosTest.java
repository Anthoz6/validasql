package co.edu.curn.validasql;

import co.edu.curn.validasql.servicio.ResultadoAnalisis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.curn.validasql.Escenario.analizar;
import static co.edu.curn.validasql.Escenario.reporte;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Los diez casos nuevos de la sección 10.4 del documento de diseño: las reglas que el anexo no
 * ilustraba (M01, M04, M07, M08) y los cinco ajustes al modelo formal de la sección 9.
 */
class CasosNuevosTest {

    @Test
    @DisplayName("C01: M01 tabla inexistente, con sugerencia «estudiante»")
    void c01TablaInexistente() {
        assertEquals(List.of(
                "[M01] Error semántico (línea 1, columna 20). La tabla «estudiantes» no existe. ¿Quisiste decir «estudiante»?"),
                reporte(analizar("SELECT nombre FROM estudiantes;"), 1));
    }

    @Test
    @DisplayName("C02: M04 calificador que no es tabla de la consulta")
    void c02CalificadorInvalido() {
        assertEquals(List.of(
                "[M04] Error semántico (línea 1, columna 8). «programa» no es una tabla de esta consulta."),
                reporte(analizar("SELECT programa.nombre FROM estudiante;"), 1));
    }

    @Test
    @DisplayName("C03: M07 falta un valor para una columna NOT NULL")
    void c03FaltaValorNotNull() {
        assertEquals(List.of(
                "[M07] Error semántico (línea 1, columna 39). Falta un valor para la columna «nombre», que es NOT NULL."),
                reporte(analizar("INSERT INTO estudiante (id, semestre) VALUES (2, 3);"), 1));
    }

    @Test
    @DisplayName("C04: M07 NULL en una columna NOT NULL")
    void c04NuloEnNotNull() {
        assertEquals(List.of(
                "[M07] Error semántico (línea 1, columna 32). La columna «nombre» es NOT NULL y no puede recibir NULL."),
                reporte(analizar("UPDATE estudiante SET nombre = NULL WHERE id = 1;"), 1));
    }

    @Test
    @DisplayName("C05: M08 tabla repetida")
    void c05TablaRepetida() {
        assertEquals(List.of(
                "[M08] Error semántico (línea 1, columna 14). La tabla «programa» ya existe."),
                reporte(analizar("CREATE TABLE programa (id INT PRIMARY KEY);"), 1));
    }

    @Test
    @DisplayName("C06: M06 INSERT sin lista de columnas con otra cantidad de valores")
    void c06CantidadSinLista() {
        assertEquals(List.of(
                "[M06] Error semántico (línea 1, columna 22). La tabla «programa» tiene 2 columnas pero la sentencia entrega 1 valor."),
                reporte(analizar("INSERT INTO programa VALUES (3);"), 1));
    }

    @Test
    @DisplayName("C07: M05 texto más largo que VARCHAR(n)")
    void c07TextoDemasiadoLargo() {
        String texto = "Tecnología en Desarrollo de Sistemas de Información y de Software";
        assertEquals(65, texto.length(), "el caso del diseño habla de 65 caracteres");
        assertEquals(List.of(
                "[M05] Error semántico (línea 1, columna 33). El texto tiene 65 caracteres y la columna «nombre» admite como máximo 60."),
                reporte(analizar("INSERT INTO programa VALUES (3, '" + texto + "');"), 1));
    }

    @Test
    @DisplayName("C08: M08 columna asignada dos veces en un UPDATE")
    void c08ColumnaRepetidaEnUpdate() {
        assertEquals(List.of(
                "[M08] Error semántico (línea 1, columna 37). La columna «semestre» se asigna más de una vez."),
                reporte(analizar("UPDATE estudiante SET semestre = 5, semestre = 6 WHERE id = 1;"), 1));
    }

    @Test
    @DisplayName("C09: una cadena sin cerrar cierra la sentencia al final de la línea; la siguiente es válida")
    void c09CadenaSinCerrarNoTragaLaSiguiente() {
        ResultadoAnalisis resultado = analizar(
                "SELECT nombre FROM estudiante WHERE nombre = 'Ana;\nDELETE FROM estudiante WHERE id = 1;");

        assertEquals(2, resultado.consultas().size());
        assertEquals(List.of(
                "[L02] Error léxico (línea 1, columna 46). Cadena sin cerrar: falta la comilla simple de cierre antes del fin de la línea."),
                reporte(resultado, 1));
        assertEquals(List.of(), reporte(resultado, 2));
    }

    @Test
    @DisplayName("C10: L01 con el mensaje propio para letras con tilde o ñ")
    void c10LetraConTilde() {
        assertEquals(List.of(
                "[L01] Error léxico (línea 1, columna 9). Carácter no válido «ñ»: los nombres de tablas y columnas no pueden llevar tildes ni ñ."),
                reporte(analizar("SELECT año FROM estudiante;"), 1));
    }
}
