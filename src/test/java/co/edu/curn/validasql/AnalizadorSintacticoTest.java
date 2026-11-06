package co.edu.curn.validasql;

import co.edu.curn.validasql.arbol.CrearTabla;
import co.edu.curn.validasql.arbol.DefColumna;
import co.edu.curn.validasql.arbol.ImpresorArbol;
import co.edu.curn.validasql.arbol.Insercion;
import co.edu.curn.validasql.arbol.Actualizacion;
import co.edu.curn.validasql.arbol.TipoDato;
import co.edu.curn.validasql.lexico.AnalizadorLexico;
import co.edu.curn.validasql.lexico.Segmentador;
import co.edu.curn.validasql.reporte.FormateadorReporte;
import co.edu.curn.validasql.sintactico.AnalizadorSintactico;
import co.edu.curn.validasql.sintactico.Motivo;
import co.edu.curn.validasql.sintactico.PasoTraza;
import co.edu.curn.validasql.sintactico.ResultadoSintactico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del parser LL(1): árbol, precedencia, mensajes S01 y traza. */
class AnalizadorSintacticoTest {

    private static ResultadoSintactico parsear(String sentencia) {
        var lexico = new AnalizadorLexico().analizar(sentencia);
        var unidades = new Segmentador().separar(lexico);
        return new AnalizadorSintactico().analizar(unidades.get(0));
    }

    private static String arbol(String sentencia) {
        ResultadoSintactico r = parsear(sentencia);
        assertTrue(r.esValido(), () -> FormateadorReporte.formatear(r.error()));
        return ImpresorArbol.imprimir(r.arbol());
    }

    private static String error(String sentencia) {
        ResultadoSintactico r = parsear(sentencia);
        assertFalse(r.esValido(), "se esperaba un error sintáctico");
        assertNull(r.arbol());
        return FormateadorReporte.formatear(r.error());
    }

    // ------------------------------------------------------------------ sentencias válidas

    @Test
    @DisplayName("CREATE TABLE: columnas, tipos, longitud y restricciones")
    void crearTabla() {
        ResultadoSintactico r = parsear(
                "CREATE TABLE docente (id INT PRIMARY KEY, nombre VARCHAR(60) NOT NULL, activo BOOLEAN);");
        CrearTabla c = assertInstanceOf(CrearTabla.class, r.arbol());

        assertEquals("docente", c.nombre().lexema());
        List<DefColumna> columnas = c.columnas();
        assertEquals(3, columnas.size());
        assertEquals(TipoDato.INT, columnas.get(0).tipo());
        assertTrue(columnas.get(0).clavePrimaria());
        assertEquals(TipoDato.VARCHAR, columnas.get(1).tipo());
        assertEquals(60, columnas.get(1).longitud());
        assertTrue(columnas.get(1).notNull());
        assertFalse(columnas.get(2).notNull());
    }

    @Test
    @DisplayName("Restr admite varias restricciones seguidas")
    void restriccionesEncadenadas() {
        CrearTabla c = assertInstanceOf(CrearTabla.class,
                parsear("CREATE TABLE t (id INT NOT NULL PRIMARY KEY);").arbol());
        assertTrue(c.columnas().get(0).notNull());
        assertTrue(c.columnas().get(0).clavePrimaria());
    }

    @Test
    @DisplayName("SELECT con JOIN, WHERE y ORDER BY")
    void selectCompleto() {
        assertEquals("""
                Select
                ├─ columnas: Col(estudiante.nombre), Col(programa.nombre)
                ├─ FROM: estudiante
                ├─ INNER JOIN: programa
                │  └─ ON
                │     └─ Comparación (=)
                │        ├─ Col(estudiante.id_programa)
                │        └─ Col(programa.id)
                ├─ WHERE
                │  └─ Comparación (>=)
                │     ├─ Col(estudiante.semestre)
                │     └─ NUM_ENT(4)
                └─ ORDER BY: Col(estudiante.nombre) DESC""",
                arbol("""
                        SELECT estudiante.nombre, programa.nombre
                        FROM estudiante INNER JOIN programa ON estudiante.id_programa = programa.id
                        WHERE estudiante.semestre >= 4 ORDER BY estudiante.nombre DESC;"""));
    }

    @Test
    @DisplayName("SELECT *")
    void selectAsterisco() {
        assertEquals("Select\n├─ columnas: *\n└─ FROM: estudiante", arbol("SELECT * FROM estudiante;"));
    }

    @Test
    @DisplayName("INSERT con y sin lista de columnas")
    void insert() {
        Insercion con = assertInstanceOf(Insercion.class,
                parsear("INSERT INTO estudiante (id, nombre) VALUES (1, 'Ana');").arbol());
        assertEquals(2, con.columnas().size());
        assertEquals(2, con.valores().size());
        assertEquals("VALUES", con.values().lexema());

        Insercion sin = assertInstanceOf(Insercion.class,
                parsear("INSERT INTO estudiante VALUES (1, 'Ana', 3, NULL);").arbol());
        assertNull(sin.columnas(), "sin lista, la parte opcional queda en null");
        assertEquals(4, sin.valores().size());
    }

    @Test
    @DisplayName("UPDATE con varias asignaciones y DELETE")
    void updateYDelete() {
        Actualizacion u = assertInstanceOf(Actualizacion.class,
                parsear("UPDATE estudiante SET semestre = 5, nombre = 'Ana' WHERE id = 1;").arbol());
        assertEquals(2, u.asignaciones().size());
        assertEquals("Delete\n├─ FROM: estudiante\n└─ WHERE\n   └─ IS NOT NULL\n      └─ Col(semestre)",
                arbol("DELETE FROM estudiante WHERE semestre IS NOT NULL;"));
    }

    // ------------------------------------------------------------------ condiciones

    @Test
    @DisplayName("Precedencia: NOT liga más que AND y AND más que OR")
    void precedencia() {
        assertEquals("""
                Delete
                ├─ FROM: t
                └─ WHERE
                   └─ Lógica (OR)
                      ├─ Comparación (=)
                      │  ├─ Col(a)
                      │  └─ NUM_ENT(1)
                      └─ Lógica (AND)
                         ├─ Comparación (=)
                         │  ├─ Col(b)
                         │  └─ NUM_ENT(2)
                         └─ Negación (NOT)
                            └─ Comparación (=)
                               ├─ Col(c)
                               └─ NUM_ENT(3)""",
                arbol("DELETE FROM t WHERE a = 1 OR b = 2 AND NOT c = 3;"));
    }

    @Test
    @DisplayName("AND y OR asocian a la izquierda: a OR b OR c es (a OR b) OR c")
    void asociaALaIzquierda() {
        assertEquals("""
                Delete
                ├─ FROM: t
                └─ WHERE
                   └─ Lógica (OR)
                      ├─ Lógica (OR)
                      │  ├─ Comparación (=)
                      │  │  ├─ Col(a)
                      │  │  └─ NUM_ENT(1)
                      │  └─ Comparación (=)
                      │     ├─ Col(b)
                      │     └─ NUM_ENT(2)
                      └─ Comparación (=)
                         ├─ Col(c)
                         └─ NUM_ENT(3)""",
                arbol("DELETE FROM t WHERE a = 1 OR b = 2 OR c = 3;"));
    }

    @Test
    @DisplayName("Los paréntesis cambian el orden: (a OR b) AND c")
    void parentesis() {
        assertEquals("""
                Delete
                ├─ FROM: t
                └─ WHERE
                   └─ Lógica (AND)
                      ├─ Lógica (OR)
                      │  ├─ Comparación (=)
                      │  │  ├─ Col(a)
                      │  │  └─ NUM_ENT(1)
                      │  └─ Comparación (=)
                      │     ├─ Col(b)
                      │     └─ NUM_ENT(2)
                      └─ IS NULL
                         └─ Col(c)""",
                arbol("DELETE FROM t WHERE (a = 1 OR b = 2) AND c IS NULL;"));
    }

    @Test
    @DisplayName("Un literal puede ir a la izquierda de una comparación")
    void literalAIzquierda() {
        assertEquals("Delete\n├─ FROM: t\n└─ WHERE\n   └─ Comparación (<>)\n      ├─ CADENA('x')\n      └─ Col(a)",
                arbol("DELETE FROM t WHERE 'x' <> a;"));
    }

    // ------------------------------------------------------------------ errores S01

    @Test
    @DisplayName("S01 al faltar el punto y coma: se señala justo después del último token")
    void errorAlFinalDeLaSentencia() {
        assertEquals("[S01] Error sintáctico (línea 1, columna 16). "
                        + "Se esperaba ';', INNER, WHERE u ORDER, pero se encontró el final de la sentencia.",
                error("SELECT a FROM t"));
    }

    @Test
    @DisplayName("S01 sobrante después de una condición: los esperados salen en el orden de los tokens")
    void errorTrasUnaCondicion() {
        assertEquals("[S01] Error sintáctico (línea 1, columna 29). "
                        + "Se esperaba ';', ORDER, AND u OR, pero se encontró el identificador «foo».",
                error("SELECT a FROM t WHERE x = 1 foo;"));
    }

    @Test
    @DisplayName("S01 en CREATE TABLE con paréntesis sin cerrar")
    void errorEnCrearTabla() {
        assertEquals("[S01] Error sintáctico (línea 1, columna 23). "
                        + "Se esperaba ',', ')', PRIMARY o NOT, pero se encontró el símbolo «;».",
                error("CREATE TABLE t (id INT;"));
    }

    @Test
    @DisplayName("S01 con VARCHAR sin longitud numérica")
    void errorEnLongitud() {
        assertEquals("[S01] Error sintáctico (línea 1, columna 27). "
                        + "Se esperaba un número entero, pero se encontró el identificador «x».",
                error("CREATE TABLE t (a VARCHAR(x));"));
    }

    @Test
    @DisplayName("S01 al empezar con una palabra que no inicia sentencia")
    void errorAlInicio() {
        assertEquals("[S01] Error sintáctico (línea 1, columna 1). "
                        + "Se esperaba CREATE, SELECT, INSERT, UPDATE o DELETE, pero se encontró el identificador «hola».",
                error("hola mundo;"));
    }

    @Test
    @DisplayName("S01 al usar una palabra reservada como nombre de tabla")
    void errorConPalabraReservada() {
        assertEquals("[S01] Error sintáctico (línea 1, columna 13). "
                        + "Se esperaba un identificador, pero se encontró la palabra reservada «SELECT».",
                error("DELETE FROM select;"));
    }

    @Test
    @DisplayName("S01 en una comparación sin segundo operando")
    void errorSinSegundoOperando() {
        assertEquals("[S01] Error sintáctico (línea 1, columna 34). Se esperaba un identificador, un número entero, "
                        + "un número decimal, una cadena de texto, NULL, TRUE o FALSE, pero se encontró el símbolo «;».",
                error("DELETE FROM t WHERE a = 1 AND b =;"));
    }

    // ------------------------------------------------------------------ traza

    @Test
    @DisplayName("Traza: el primer paso es Sent → Select ';' por PRIMERO")
    void trazaPrimerPaso() {
        List<PasoTraza> traza = parsear("SELECT nombre FROM estudiante WHERE semestre >= 4;").traza();
        PasoTraza primero = traza.get(0);
        assertEquals(new PasoTraza(1, "Sent", "SELECT", "Sent → Select ';'", Motivo.PRIMERO), primero);
    }

    @Test
    @DisplayName("Traza: ColQ, ColsR y Join eligen ε por SIGUIENTE en el ejemplo 1 del anexo")
    void trazaEpsilonPorSiguiente() {
        List<PasoTraza> traza = parsear("SELECT nombre FROM estudiante WHERE semestre >= 4;").traza();
        List<String> epsilonIniciales = traza.stream().skip(3).limit(4)
                .map(p -> p.noTerminal() + ":" + p.motivo() + ":" + p.token()).toList();
        assertEquals(List.of("Col:PRIMERO:nombre", "ColQ:SIGUIENTE:FROM", "ColsR:SIGUIENTE:FROM",
                "Join:SIGUIENTE:WHERE"), epsilonIniciales);
    }

    @Test
    @DisplayName("Traza: con el token equivocado, ε se elige por DESCARTE y el error sale en el consumir siguiente")
    void trazaDescarte() {
        ResultadoSintactico r = parsear("SELECT nombre estudiante;");
        List<PasoTraza> traza = r.traza();
        PasoTraza ultimo = traza.get(traza.size() - 1);

        assertEquals("ColsR", ultimo.noTerminal());
        assertEquals(Motivo.DESCARTE, ultimo.motivo());
        assertEquals("estudiante", ultimo.token());
        assertEquals(traza.size(), ultimo.paso(), "los pasos se numeran desde 1 sin saltos");
    }
}
