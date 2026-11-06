package co.edu.curn.validasql.sintactico;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Conjuntos PRIMERO y SIGUIENTE de la gramática del anexo, calculados con el algoritmo de punto fijo.
 *
 * <p>El parser consulta SIGUIENTE para registrar en la traza si una producción vacía se eligió
 * «por SIGUIENTE» o «por DESCARTE». La misma clase construye la tabla LL(1) para comprobar que la
 * gramática no tiene conflictos (lo verifica {@code GramaticaLL1Test}).</p>
 *
 * <p>Los terminales se llaman igual que los {@link co.edu.curn.validasql.lexico.TipoToken}. El
 * símbolo {@code $} marca el fin de la entrada y solo aparece en SIGUIENTE(Prog).</p>
 */
public final class ConjuntosLL1 {

    /** Gramática del anexo, sección 5. {@code ε} es la cadena vacía. */
    static final String GRAMATICA = """
            Prog     → Sent Prog | ε
            Sent     → Crear ';' | Select ';' | Insert ';' | Update ';' | Delete ';'
            Crear    → CREATE TABLE IDENT '(' DefCol DefColR ')'
            DefColR  → ',' DefCol DefColR | ε
            DefCol   → IDENT Tipo Restr
            Tipo     → INT | DECIMAL | VARCHAR '(' NUM_ENT ')' | DATE | BOOLEAN
            Restr    → PRIMARY KEY Restr | NOT NULL Restr | ε
            Select   → SELECT Cols FROM IDENT Join Where Orden
            Cols     → '*' | Col ColsR
            ColsR    → ',' Col ColsR | ε
            Col      → IDENT ColQ
            ColQ     → '.' IDENT | ε
            Join     → INNER JOIN IDENT ON Cond | ε
            Where    → WHERE Cond | ε
            Orden    → ORDER BY Col Dir | ε
            Dir      → ASC | DESC | ε
            Insert   → INSERT INTO IDENT InsCols VALUES '(' Lit LitR ')'
            InsCols  → '(' IDENT IdR ')' | ε
            IdR      → ',' IDENT IdR | ε
            LitR     → ',' Lit LitR | ε
            Update   → UPDATE IDENT SET Asig AsigR Where
            Asig     → IDENT '=' Lit
            AsigR    → ',' Asig AsigR | ε
            Delete   → DELETE FROM IDENT Where
            Cond     → CondY CondO
            CondO    → OR CondY CondO | ε
            CondY    → CondN CondYR
            CondYR   → AND CondN CondYR | ε
            CondN    → NOT CondN | Prim
            Prim     → '(' Cond ')' | Opnd PrimR
            PrimR    → OpRel Opnd | IS IsR
            IsR      → NULL | NOT NULL
            Opnd     → Col | Lit
            OpRel    → '=' | '<>' | '<' | '>' | '<=' | '>='
            Lit      → NUM_ENT | NUM_DEC | CADENA | TRUE | FALSE | NULL
            """;

    /** Nombre del token que corresponde a cada terminal escrito entre comillas en la gramática. */
    private static final Map<String, String> SIMBOLOS = Map.ofEntries(
            Map.entry("'('", "PAR_IZQ"), Map.entry("')'", "PAR_DER"), Map.entry("','", "COMA"),
            Map.entry("';'", "PUNTOCOMA"), Map.entry("'.'", "PUNTO"), Map.entry("'*'", "ASTERISCO"),
            Map.entry("'='", "IGUAL"), Map.entry("'<>'", "DISTINTO"), Map.entry("'<'", "MENOR"),
            Map.entry("'>'", "MAYOR"), Map.entry("'<='", "MENORIG"), Map.entry("'>='", "MAYORIG"));

    private static final String FIN_DE_ENTRADA = "$";
    private static final String SIMBOLO_INICIAL = "Prog";

    private static final ConjuntosLL1 INSTANCIA = new ConjuntosLL1(GRAMATICA);

    /** Cada no terminal con sus producciones; cada producción es una lista de símbolos (vacía = ε). */
    private final Map<String, List<List<String>>> producciones = new LinkedHashMap<>();
    private final Set<String> anulables = new TreeSet<>();
    private final Map<String, Set<String>> primero = new LinkedHashMap<>();
    private final Map<String, Set<String>> siguiente = new LinkedHashMap<>();

    private ConjuntosLL1(String gramatica) {
        leerGramatica(gramatica);
        calcularAnulables();
        calcularPrimero();
        calcularSiguiente();
    }

    /** La gramática de ValidaSQL, calculada una sola vez. */
    public static ConjuntosLL1 gramatica() {
        return INSTANCIA;
    }

    // ------------------------------------------------------------------ consultas

    public Set<String> noTerminales() {
        return Collections.unmodifiableSet(producciones.keySet());
    }

    public Set<String> terminales() {
        Set<String> resultado = new TreeSet<>();
        for (List<List<String>> alternativas : producciones.values()) {
            for (List<String> produccion : alternativas) {
                for (String simbolo : produccion) {
                    if (esTerminal(simbolo)) {
                        resultado.add(simbolo);
                    }
                }
            }
        }
        return resultado;
    }

    public int cantidadProducciones() {
        return producciones.values().stream().mapToInt(List::size).sum();
    }

    public Set<String> anulables() {
        return Collections.unmodifiableSet(anulables);
    }

    public boolean esAnulable(String noTerminal) {
        return anulables.contains(noTerminal);
    }

    /** PRIMERO del no terminal, sin ε. */
    public Set<String> primero(String noTerminal) {
        return Collections.unmodifiableSet(primero.get(noTerminal));
    }

    /** SIGUIENTE del no terminal. */
    public Set<String> siguiente(String noTerminal) {
        return Collections.unmodifiableSet(siguiente.get(noTerminal));
    }

    /**
     * Celdas de la tabla LL(1) que tienen más de una producción. Si la gramática es LL(1) la lista
     * está vacía. Cada elemento describe el no terminal, el terminal y las producciones en choque.
     */
    public List<String> conflictos() {
        Map<String, List<String>> tabla = new TreeMap<>();
        for (Map.Entry<String, List<List<String>>> entrada : producciones.entrySet()) {
            String noTerminal = entrada.getKey();
            for (List<String> produccion : entrada.getValue()) {
                Set<String> seleccion = new TreeSet<>(primeroDeSecuencia(produccion));
                if (esSecuenciaAnulable(produccion)) {
                    seleccion.addAll(siguiente.get(noTerminal));
                }
                for (String terminal : seleccion) {
                    tabla.computeIfAbsent(noTerminal + " con " + terminal, k -> new ArrayList<>())
                            .add(noTerminal + " → " + (produccion.isEmpty() ? "ε" : String.join(" ", produccion)));
                }
            }
        }
        List<String> conflictos = new ArrayList<>();
        tabla.forEach((celda, lista) -> {
            if (lista.size() > 1) {
                conflictos.add(celda + ": " + lista);
            }
        });
        return conflictos;
    }

    // ------------------------------------------------------------------ lectura de la gramática

    private void leerGramatica(String texto) {
        for (String linea : texto.split("\n")) {
            if (linea.isBlank()) {
                continue;
            }
            String[] partes = linea.split("→");
            String izquierda = partes[0].trim();
            List<List<String>> alternativas = new ArrayList<>();
            for (String alternativa : partes[1].split("\\|")) {
                List<String> simbolos = new ArrayList<>();
                for (String simbolo : alternativa.trim().split("\\s+")) {
                    if (!simbolo.equals("ε")) {
                        simbolos.add(SIMBOLOS.getOrDefault(simbolo, simbolo));
                    }
                }
                alternativas.add(simbolos);
            }
            producciones.put(izquierda, alternativas);
        }
    }

    private boolean esTerminal(String simbolo) {
        return !producciones.containsKey(simbolo);
    }

    // ------------------------------------------------------------------ puntos fijos

    /** Un no terminal es anulable si alguna de sus producciones está formada solo por anulables. */
    private void calcularAnulables() {
        boolean cambio = true;
        while (cambio) {
            cambio = false;
            for (Map.Entry<String, List<List<String>>> entrada : producciones.entrySet()) {
                for (List<String> produccion : entrada.getValue()) {
                    if (esSecuenciaAnulable(produccion) && anulables.add(entrada.getKey())) {
                        cambio = true;
                    }
                }
            }
        }
    }

    private boolean esSecuenciaAnulable(List<String> simbolos) {
        return simbolos.stream().allMatch(s -> !esTerminal(s) && anulables.contains(s));
    }

    /** PRIMERO(A) incluye los primeros terminales de cada producción, avanzando mientras haya anulables. */
    private void calcularPrimero() {
        producciones.keySet().forEach(nt -> primero.put(nt, new LinkedHashSet<>()));
        boolean cambio = true;
        while (cambio) {
            cambio = false;
            for (Map.Entry<String, List<List<String>>> entrada : producciones.entrySet()) {
                for (List<String> produccion : entrada.getValue()) {
                    if (primero.get(entrada.getKey()).addAll(primeroDeSecuencia(produccion))) {
                        cambio = true;
                    }
                }
            }
        }
    }

    /** PRIMERO de una secuencia de símbolos, sin ε, con los conjuntos calculados hasta ahora. */
    private Set<String> primeroDeSecuencia(List<String> simbolos) {
        Set<String> resultado = new LinkedHashSet<>();
        for (String simbolo : simbolos) {
            if (esTerminal(simbolo)) {
                resultado.add(simbolo);
                break;
            }
            resultado.addAll(primero.getOrDefault(simbolo, Set.of()));
            if (!anulables.contains(simbolo)) {
                break;
            }
        }
        return resultado;
    }

    /**
     * Para cada producción {@code A → α B β}: PRIMERO(β) entra en SIGUIENTE(B), y si β puede ser ε,
     * también entra SIGUIENTE(A). El símbolo inicial recibe el fin de la entrada.
     */
    private void calcularSiguiente() {
        producciones.keySet().forEach(nt -> siguiente.put(nt, new LinkedHashSet<>()));
        siguiente.get(SIMBOLO_INICIAL).add(FIN_DE_ENTRADA);
        boolean cambio = true;
        while (cambio) {
            cambio = false;
            for (Map.Entry<String, List<List<String>>> entrada : producciones.entrySet()) {
                String cabeza = entrada.getKey();
                for (List<String> produccion : entrada.getValue()) {
                    for (int i = 0; i < produccion.size(); i++) {
                        String simbolo = produccion.get(i);
                        if (esTerminal(simbolo)) {
                            continue;
                        }
                        List<String> resto = produccion.subList(i + 1, produccion.size());
                        if (siguiente.get(simbolo).addAll(primeroDeSecuencia(resto))) {
                            cambio = true;
                        }
                        if (esSecuenciaAnulable(resto) && siguiente.get(simbolo).addAll(siguiente.get(cabeza))) {
                            cambio = true;
                        }
                    }
                }
            }
        }
    }
}
