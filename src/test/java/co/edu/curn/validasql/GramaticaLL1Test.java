package co.edu.curn.validasql;

import co.edu.curn.validasql.lexico.TipoToken;
import co.edu.curn.validasql.sintactico.ConjuntosLL1;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica la gramática del anexo (secciones 5 y 6) calculando PRIMERO y SIGUIENTE con el algoritmo
 * de punto fijo de {@link ConjuntosLL1}: las cantidades, los 15 no terminales anulables, los
 * conjuntos que publica el anexo y la ausencia de conflictos en la tabla LL(1).
 */
class GramaticaLL1Test {

    private final ConjuntosLL1 g = ConjuntosLL1.gramatica();

    @Test
    @DisplayName("35 no terminales, 48 terminales y 76 producciones")
    void cantidades() {
        assertEquals(35, g.noTerminales().size());
        assertEquals(48, g.terminales().size());
        assertEquals(76, g.cantidadProducciones());
    }

    @Test
    @DisplayName("Los terminales de la gramática son exactamente los 48 tipos de token")
    void terminalesSonLosTokens() {
        Set<String> tokens = Arrays.stream(TipoToken.values()).map(Enum::name).collect(Collectors.toCollection(TreeSet::new));
        assertEquals(tokens, g.terminales());
    }

    @Test
    @DisplayName("Sin conflictos: ninguna celda de la tabla LL(1) tiene más de una producción")
    void sinConflictos() {
        assertEquals(List.of(), g.conflictos());
    }

    @Test
    @DisplayName("15 no terminales anulables, los de la tabla del anexo")
    void anulables() {
        assertEquals(15, g.anulables().size());
        assertEquals(new TreeSet<>(Set.of("Prog", "DefColR", "Restr", "ColsR", "ColQ", "Join", "Where", "Orden",
                "Dir", "InsCols", "IdR", "LitR", "AsigR", "CondO", "CondYR")), g.anulables());
    }

    @Test
    @DisplayName("En los anulables, PRIMERO y SIGUIENTE son disjuntos (segunda condición LL(1))")
    void primeroYSiguienteDisjuntos() {
        for (String nt : g.anulables()) {
            Set<String> comun = new TreeSet<>(g.primero(nt));
            comun.retainAll(g.siguiente(nt));
            assertTrue(comun.isEmpty(), nt + " comparte " + comun);
        }
    }

    @Test
    @DisplayName("SIGUIENTE de los anulables coincide con la tabla de la sección 6 del anexo")
    void siguienteDelAnexo() {
        assertSiguiente("Prog", "$");
        assertSiguiente("DefColR", "PAR_DER");
        assertSiguiente("Restr", "PAR_DER", "COMA");
        assertSiguiente("ColsR", "FROM");
        assertSiguiente("ColQ", "AND", "ASC", "DESC", "FROM", "IS", "OR", "ORDER", "WHERE", "PAR_DER", "COMA",
                "PUNTOCOMA", "MENOR", "MENORIG", "DISTINTO", "IGUAL", "MAYOR", "MAYORIG");
        assertSiguiente("Join", "ORDER", "WHERE", "PUNTOCOMA");
        assertSiguiente("Where", "ORDER", "PUNTOCOMA");
        assertSiguiente("Orden", "PUNTOCOMA");
        assertSiguiente("Dir", "PUNTOCOMA");
        assertSiguiente("InsCols", "VALUES");
        assertSiguiente("IdR", "PAR_DER");
        assertSiguiente("LitR", "PAR_DER");
        assertSiguiente("AsigR", "WHERE", "PUNTOCOMA");
        assertSiguiente("CondO", "ORDER", "WHERE", "PAR_DER", "PUNTOCOMA");
        assertSiguiente("CondYR", "OR", "ORDER", "WHERE", "PAR_DER", "PUNTOCOMA");
    }

    @Test
    @DisplayName("PRIMERO de los anulables coincide con la tabla de la sección 6 del anexo")
    void primeroDelAnexo() {
        assertEquals(Set.of("CREATE", "DELETE", "INSERT", "SELECT", "UPDATE"), g.primero("Prog"));
        assertEquals(Set.of("COMA"), g.primero("DefColR"));
        assertEquals(Set.of("NOT", "PRIMARY"), g.primero("Restr"));
        assertEquals(Set.of("PUNTO"), g.primero("ColQ"));
        assertEquals(Set.of("INNER"), g.primero("Join"));
        assertEquals(Set.of("ASC", "DESC"), g.primero("Dir"));
        assertEquals(Set.of("PAR_IZQ"), g.primero("InsCols"));
        assertEquals(Set.of("OR"), g.primero("CondO"));
        assertEquals(Set.of("AND"), g.primero("CondYR"));
    }

    private void assertSiguiente(String noTerminal, String... esperados) {
        assertEquals(new TreeSet<>(Set.of(esperados)), new TreeSet<>(g.siguiente(noTerminal)), "SIGUIENTE(" + noTerminal + ")");
    }
}
