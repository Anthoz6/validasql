package co.edu.curn.validasql;

import co.edu.curn.validasql.lexico.AnalizadorLexico;
import co.edu.curn.validasql.lexico.ResultadoLexico;
import co.edu.curn.validasql.lexico.Segmentador;
import co.edu.curn.validasql.lexico.TipoToken;
import co.edu.curn.validasql.lexico.UnidadSentencia;
import co.edu.curn.validasql.reporte.CodigoDiagnostico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del AFD (q0 a q18) y del segmentador. */
class AnalizadorLexicoTest {

    private static ResultadoLexico leer(String texto) {
        return new AnalizadorLexico().analizar(texto);
    }

    private static List<TipoToken> tipos(String texto) {
        return leer(texto).tokens().stream().map(t -> t.tipo()).toList();
    }

    @Test
    @DisplayName("Los 32 lexemas reservados se reconocen sin distinguir mayúsculas")
    void palabrasReservadas() {
        String reservadas = "CREATE TABLE PRIMARY KEY NOT NULL INT DECIMAL VARCHAR DATE BOOLEAN SELECT FROM INNER JOIN "
                + "ON WHERE ORDER BY ASC DESC INSERT INTO VALUES UPDATE SET DELETE AND OR IS TRUE FALSE";
        assertEquals(32, reservadas.split(" ").length);
        assertEquals(32, TipoToken.values().length - 16);

        List<TipoToken> enMayusculas = tipos(reservadas);
        List<TipoToken> enMinusculas = tipos(reservadas.toLowerCase());
        assertEquals(enMayusculas, enMinusculas);
        assertTrue(enMayusculas.stream().allMatch(TipoToken::esPalabraReservada));
        assertEquals(TipoToken.CREATE, enMayusculas.get(0));
    }

    @Test
    @DisplayName("Un identificador que empieza con una palabra reservada sigue siendo identificador")
    void identificadorConPrefijoReservado() {
        assertEquals(List.of(TipoToken.IDENT, TipoToken.IDENT, TipoToken.IDENT), tipos("selecto tabla1 id_programa"));
    }

    @Test
    @DisplayName("Lexema más largo: <=, <>, >= son un solo token")
    void operadoresDeDosCaracteres() {
        assertEquals(List.of(TipoToken.MENORIG, TipoToken.DISTINTO, TipoToken.MAYORIG, TipoToken.MENOR,
                TipoToken.MAYOR, TipoToken.IGUAL), tipos("<= <> >= < > ="));
        assertEquals(List.of(TipoToken.MENORIG, TipoToken.IGUAL), tipos("<=="));
    }

    @Test
    @DisplayName("Signos de puntuación y asterisco")
    void puntuacion() {
        assertEquals(List.of(TipoToken.COMA, TipoToken.PUNTOCOMA, TipoToken.PAR_IZQ, TipoToken.PAR_DER,
                TipoToken.PUNTO, TipoToken.ASTERISCO), tipos(", ; ( ) . *"));
    }

    @Test
    @DisplayName("Números: entero, decimal, y 12abc se parte en NUM_ENT e IDENT")
    void numeros() {
        assertEquals(List.of(TipoToken.NUM_ENT, TipoToken.NUM_DEC, TipoToken.NUM_ENT), tipos("120 3.5 0"));
        assertEquals(List.of(TipoToken.NUM_ENT, TipoToken.IDENT), tipos("12abc"));
        assertEquals("0.25", leer("0.25").tokens().get(0).lexema());
    }

    @Test
    @DisplayName("Un punto entre identificadores es PUNTO; después de un entero forma un decimal")
    void puntoDeCalificadorYDecimal() {
        assertEquals(List.of(TipoToken.IDENT, TipoToken.PUNTO, TipoToken.IDENT), tipos("estudiante.nombre"));
        assertEquals(List.of(TipoToken.NUM_DEC), tipos("3.14"));
    }

    @Test
    @DisplayName("Una cadena conserva sus comillas y acepta tildes, ñ y símbolos")
    void cadenas() {
        ResultadoLexico r = leer("'Tecnología; año = <>'");
        assertEquals(1, r.tokens().size());
        assertEquals(TipoToken.CADENA, r.tokens().get(0).tipo());
        assertEquals("'Tecnología; año = <>'", r.tokens().get(0).lexema());
        assertTrue(r.errores().isEmpty());
    }

    @Test
    @DisplayName("Línea y columna empiezan en 1 y cada salto de línea aumenta la línea")
    void posiciones() {
        ResultadoLexico r = leer("SELECT a\n  FROM t");
        assertEquals("SELECT 1:1, a 1:8, FROM 2:3, t 2:8", r.tokens().stream()
                .map(t -> t.lexema() + " " + t.linea() + ":" + t.columna()).collect(Collectors.joining(", ")));
    }

    @Test
    @DisplayName("L01 descarta el carácter y sigue leyendo")
    void l01SigueLeyendo() {
        ResultadoLexico r = leer("a # b @ c");
        assertEquals(3, r.tokens().size());
        assertEquals(2, r.errores().size());
        assertEquals(3, r.errores().get(0).columna());
        assertEquals(CodigoDiagnostico.L01, r.errores().get(1).codigo());
    }

    @Test
    @DisplayName("L03: el decimal incompleto no se come el carácter siguiente")
    void l03NoConsumeElSiguiente() {
        ResultadoLexico r = leer("3.;");
        assertEquals(List.of(TipoToken.PUNTOCOMA), r.tokens().stream().map(t -> t.tipo()).toList());
        assertEquals(CodigoDiagnostico.L03, r.errores().get(0).codigo());
        assertEquals(1, r.errores().get(0).columna());
    }

    @Test
    @DisplayName("L02 al final del texto, sin salto de línea")
    void l02AlFinalDelTexto() {
        ResultadoLexico r = leer("'abc");
        assertEquals(CodigoDiagnostico.L02, r.errores().get(0).codigo());
        assertTrue(r.tokens().isEmpty());
    }

    @Test
    @DisplayName("Un texto vacío o solo con separadores no produce tokens ni errores")
    void textoVacio() {
        assertTrue(leer("").tokens().isEmpty());
        assertTrue(leer("  \n\t \r\n").tokens().isEmpty());
        assertTrue(leer(null).tokens().isEmpty());
    }

    // ------------------------------------------------------------------ segmentador

    private static List<UnidadSentencia> separar(String texto) {
        return new Segmentador().separar(leer(texto));
    }

    @Test
    @DisplayName("Segmentador: una sentencia por cada punto y coma, con su texto")
    void segmentaPorPuntoYComa() {
        List<UnidadSentencia> u = separar("SELECT a FROM t;  DELETE FROM t;\nSELECT b FROM t;");
        assertEquals(3, u.size());
        assertEquals("SELECT a FROM t;", u.get(0).texto());
        assertEquals("DELETE FROM t;", u.get(1).texto());
        assertEquals("SELECT b FROM t;", u.get(2).texto());
        assertEquals(1, u.get(0).numero());
        assertEquals(3, u.get(2).numero());
        assertEquals(2, u.get(2).linea());
        assertEquals(1, u.get(2).columna());
    }

    @Test
    @DisplayName("Segmentador: la última sentencia puede no terminar en punto y coma")
    void ultimaSinPuntoYComa() {
        List<UnidadSentencia> u = separar("SELECT a FROM t; SELECT b FROM t");
        assertEquals(2, u.size());
        assertEquals("SELECT b FROM t", u.get(1).texto());
    }

    @Test
    @DisplayName("Segmentador: cada error léxico queda en la sentencia donde ocurrió")
    void erroresEnSuSentencia() {
        List<UnidadSentencia> u = separar("SELECT # FROM t; SELECT a FROM t; SELECT b FROM t 3.;");
        assertEquals(3, u.size());
        assertEquals(1, u.get(0).erroresLexicos().size());
        assertFalse(u.get(1).tieneErroresLexicos());
        assertEquals(CodigoDiagnostico.L03, u.get(2).erroresLexicos().get(0).codigo());
    }

    @Test
    @DisplayName("Segmentador: tras un L02 la sentencia se cierra al final de esa línea")
    void l02CierraLaSentencia() {
        List<UnidadSentencia> u = separar("SELECT a FROM t WHERE a = 'Ana;\nDELETE FROM t WHERE id = 1;\nSELECT b FROM t;");
        assertEquals(3, u.size());
        assertEquals("SELECT a FROM t WHERE a = 'Ana;", u.get(0).texto());
        assertTrue(u.get(0).tieneErroresLexicos());
        assertEquals("DELETE FROM t WHERE id = 1;", u.get(1).texto());
        assertFalse(u.get(1).tieneErroresLexicos());
        assertEquals(2, u.get(1).linea());
    }

    @Test
    @DisplayName("Segmentador: un texto vacío no produce sentencias")
    void sinSentencias() {
        assertTrue(separar("").isEmpty());
        assertTrue(separar("   \n  ").isEmpty());
    }
}
