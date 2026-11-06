package co.edu.curn.validasql.semantico;

import co.edu.curn.validasql.arbol.Actualizacion;
import co.edu.curn.validasql.arbol.Comparacion;
import co.edu.curn.validasql.arbol.Condicion;
import co.edu.curn.validasql.arbol.CrearTabla;
import co.edu.curn.validasql.arbol.DefColumna;
import co.edu.curn.validasql.arbol.Eliminacion;
import co.edu.curn.validasql.arbol.EsNulo;
import co.edu.curn.validasql.arbol.Insercion;
import co.edu.curn.validasql.arbol.Literal;
import co.edu.curn.validasql.arbol.Logica;
import co.edu.curn.validasql.arbol.Negacion;
import co.edu.curn.validasql.arbol.Operando;
import co.edu.curn.validasql.arbol.ReferenciaColumna;
import co.edu.curn.validasql.arbol.Seleccion;
import co.edu.curn.validasql.arbol.Sentencia;
import co.edu.curn.validasql.lexico.TipoToken;
import co.edu.curn.validasql.lexico.Token;
import co.edu.curn.validasql.reporte.CodigoDiagnostico;
import co.edu.curn.validasql.reporte.CodigoDiagnostico.Variante;
import co.edu.curn.validasql.reporte.Diagnostico;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Analizador semántico: recorre el árbol de una sentencia válida sintácticamente y la contrasta con
 * la tabla de símbolos.
 *
 * <p>Reglas que respeta (anexo, sección 7):</p>
 * <ul>
 *   <li>Reporta todos sus errores y advertencias de la sentencia, no solo el primero.</li>
 *   <li>Si una tabla no existe (M01), no comprueba sus columnas, para no encadenar errores.</li>
 *   <li>Solo escribe en la tabla de símbolos cuando la sentencia es un CREATE TABLE sin errores.</li>
 *   <li>Devuelve los diagnósticos ordenados por posición.</li>
 * </ul>
 *
 * <p>Es una clase sin estado: se puede reutilizar para varias sentencias.</p>
 */
public final class AnalizadorSemantico {

    /** Una tabla nombrada en FROM, JOIN, UPDATE o DELETE; {@code tabla} es null si no existe. */
    private record TablaDeConsulta(Token nombre, Tabla tabla) {
    }

    /** Las tablas visibles para resolver columnas en una sentencia. */
    private record Ambito(List<TablaDeConsulta> tablas) {

        /** Hay una tabla inexistente, así que las columnas sin calificar no se pueden comprobar. */
        boolean incompleto() {
            return tablas.stream().anyMatch(t -> t.tabla() == null);
        }

        Optional<TablaDeConsulta> buscar(String calificador) {
            return tablas.stream()
                    .filter(t -> t.nombre().lexema().equalsIgnoreCase(calificador))
                    .findFirst();
        }

        List<Tabla> existentes() {
            return tablas.stream().map(TablaDeConsulta::tabla).filter(t -> t != null).toList();
        }
    }

    /** Operando de una comparación: una columna ya resuelta o un literal. */
    private record OperandoResuelto(Operando origen, Columna columna) {
        Literal literal() {
            return (Literal) origen;
        }

        boolean esColumna() {
            return columna != null;
        }
    }

    private final Sugeridor sugeridor = new Sugeridor();
    private final VerificadorTipos tipos = new VerificadorTipos();

    public List<Diagnostico> analizar(Sentencia sentencia, TablaSimbolos tabla) {
        List<Diagnostico> diagnosticos = switch (sentencia) {
            case CrearTabla c -> registrarTabla(c, tabla);
            case Seleccion s -> verificarSelect(s, tabla);
            case Insercion i -> verificarInsert(i, tabla);
            case Actualizacion a -> verificarUpdate(a, tabla);
            case Eliminacion e -> verificarDelete(e, tabla);
        };
        diagnosticos.sort(Diagnostico.POR_POSICION);
        return diagnosticos;
    }

    // ================================================================== CREATE TABLE

    /**
     * Registra la tabla si no tiene errores: nombre nuevo, columnas sin repetir y a lo sumo una clave
     * primaria. PRIMARY KEY implica NOT NULL.
     */
    private List<Diagnostico> registrarTabla(CrearTabla crear, TablaSimbolos tabla) {
        List<Diagnostico> d = new ArrayList<>();
        Token nombreTabla = crear.nombre();
        if (tabla.existe(nombreTabla.lexema())) {
            d.add(CodigoDiagnostico.M08.crearVariante(nombreTabla.linea(), nombreTabla.columna(),
                    Variante.TABLA_EXISTE, nombreTabla.lexema()));
        }

        Map<String, Columna> columnas = new LinkedHashMap<>();
        DefColumna primeraClave = null;
        for (DefColumna definicion : crear.columnas()) {
            Token nombre = definicion.nombre();
            String clave = minuscula(nombre);
            if (columnas.containsKey(clave)) {
                d.add(CodigoDiagnostico.M08.crearVariante(nombre.linea(), nombre.columna(),
                        Variante.COLUMNA_REPETIDA, nombre.lexema(), nombreTabla.lexema()));
                continue;
            }
            if (definicion.clavePrimaria()) {
                if (primeraClave != null) {
                    d.add(CodigoDiagnostico.M08.crearVariante(nombre.linea(), nombre.columna(),
                            Variante.CLAVE_REPETIDA, nombreTabla.lexema(), primeraClave.nombre().lexema()));
                } else {
                    primeraClave = definicion;
                }
            }
            columnas.put(clave, new Columna(clave, definicion.tipo(), definicion.longitud(),
                    definicion.notNull() || definicion.clavePrimaria(), definicion.clavePrimaria()));
        }

        if (d.isEmpty()) {
            tabla.registrar(new Tabla(nombreTabla.lexema(), columnas));
        }
        return d;
    }

    // ================================================================== SELECT

    private List<Diagnostico> verificarSelect(Seleccion s, TablaSimbolos simbolos) {
        List<Diagnostico> d = new ArrayList<>();

        List<TablaDeConsulta> tablas = new ArrayList<>();
        tablas.add(new TablaDeConsulta(s.tabla(), resolverTabla(s.tabla(), simbolos, d)));
        if (s.join() != null) {
            tablas.add(new TablaDeConsulta(s.join().tabla(), resolverTabla(s.join().tabla(), simbolos, d)));
        }
        Ambito ambito = new Ambito(tablas);

        if (s.asterisco() != null) {
            d.add(CodigoDiagnostico.A03.crear(s.asterisco().linea(), s.asterisco().columna()));
        }
        for (ReferenciaColumna columna : s.columnas()) {
            resolverColumna(columna, ambito, d);
        }
        if (s.join() != null) {
            verificarCondicion(s.join().condicion(), ambito, d);
        }
        if (s.where() != null) {
            verificarCondicion(s.where(), ambito, d);
        }
        if (s.orden() != null) {
            resolverColumna(s.orden().columna(), ambito, d);
        }
        return d;
    }

    // ================================================================== INSERT

    private List<Diagnostico> verificarInsert(Insercion insercion, TablaSimbolos simbolos) {
        List<Diagnostico> d = new ArrayList<>();
        Tabla tabla = resolverTabla(insercion.tabla(), simbolos, d);
        if (tabla == null) {
            return d;
        }
        Token values = insercion.values();
        List<Literal> valores = insercion.valores();

        if (insercion.columnas() == null) {
            // Sin lista: los valores van a las columnas de la tabla, en el orden en que se declararon.
            List<Columna> columnas = tabla.columnas();
            if (columnas.size() != valores.size()) {
                d.add(CodigoDiagnostico.M06.crearVariante(values.linea(), values.columna(), Variante.SIN_LISTA,
                        tabla.nombre(), cantidad(columnas.size(), "columna", "columnas"),
                        cantidad(valores.size(), "valor", "valores")));
                return d;
            }
            verificarValores(columnas, valores, d);
            return d;
        }

        // Con lista: se resuelve cada nombre; los desconocidos y repetidos quedan como null.
        List<Columna> destino = new ArrayList<>();
        Set<String> nombradas = new HashSet<>();
        boolean hayNombresDesconocidos = false;
        for (Token nombre : insercion.columnas()) {
            String clave = minuscula(nombre);
            Columna columna = tabla.columna(clave).orElse(null);
            if (!nombradas.add(clave)) {
                d.add(CodigoDiagnostico.M08.crearVariante(nombre.linea(), nombre.columna(),
                        Variante.REPETIDA_EN_LISTA, nombre.lexema()));
                columna = null;
            } else if (columna == null) {
                d.add(columnaInexistente(nombre, tabla));
                hayNombresDesconocidos = true;
            }
            destino.add(columna);
        }

        if (destino.size() != valores.size()) {
            d.add(CodigoDiagnostico.M06.crearVariante(values.linea(), values.columna(), Variante.CON_LISTA,
                    cantidad(destino.size(), "columna", "columnas"), cantidad(valores.size(), "valor", "valores")));
            return d;
        }
        verificarValores(destino, valores, d);

        // Las columnas NOT NULL que no se nombraron se quedarían sin valor. Si hubo un nombre
        // desconocido (M02) no se revisa, porque probablemente era una de esas columnas mal escrita.
        for (Columna columna : hayNombresDesconocidos ? List.<Columna>of() : tabla.columnas()) {
            if (columna.notNull() && !nombradas.contains(columna.nombre())) {
                d.add(CodigoDiagnostico.M07.crearVariante(values.linea(), values.columna(),
                        Variante.FALTA_VALOR, columna.nombre()));
            }
        }
        return d;
    }

    private void verificarValores(List<Columna> columnas, List<Literal> valores, List<Diagnostico> d) {
        for (int i = 0; i < columnas.size(); i++) {
            if (columnas.get(i) != null) {
                verificarValor(columnas.get(i), valores.get(i), d);
            }
        }
    }

    /** NULL en una columna NOT NULL es M07; cualquier otro valor se contrasta con el tipo (M05). */
    private void verificarValor(Columna columna, Literal valor, List<Diagnostico> d) {
        Token t = valor.token();
        if (valor.esNulo()) {
            if (columna.notNull()) {
                d.add(CodigoDiagnostico.M07.crearVariante(t.linea(), t.columna(),
                        Variante.NULO_EN_NOT_NULL, columna.nombre()));
            }
            return;
        }
        switch (tipos.evaluar(columna, valor, true)) {
            case COMPATIBLE -> { }
            case TIPO_INCOMPATIBLE -> d.add(CodigoDiagnostico.M05.crearVariante(t.linea(), t.columna(),
                    Variante.ASIGNACION, describir(valor), columna.nombre(), columna.tipoCompleto()));
            case TEXTO_LARGO -> d.add(CodigoDiagnostico.M05.crearVariante(t.linea(), t.columna(),
                    Variante.LONGITUD, valor.contenido().length(), columna.nombre(), columna.longitud()));
            case FECHA_INVALIDA -> d.add(CodigoDiagnostico.M05.crearVariante(t.linea(), t.columna(),
                    Variante.FECHA, t.lexema(), columna.nombre()));
        }
    }

    // ================================================================== UPDATE y DELETE

    private List<Diagnostico> verificarUpdate(Actualizacion u, TablaSimbolos simbolos) {
        List<Diagnostico> d = new ArrayList<>();
        Tabla tabla = resolverTabla(u.tabla(), simbolos, d);

        Set<String> asignadas = new HashSet<>();
        for (var asignacion : u.asignaciones()) {
            Token nombre = asignacion.columna();
            if (!asignadas.add(minuscula(nombre))) {
                d.add(CodigoDiagnostico.M08.crearVariante(nombre.linea(), nombre.columna(),
                        Variante.ASIGNADA_VARIAS, nombre.lexema()));
            }
            if (tabla == null) {
                continue;                               // la tabla no existe: no se revisan sus columnas
            }
            Columna columna = tabla.columna(nombre.lexema()).orElse(null);
            if (columna == null) {
                d.add(columnaInexistente(nombre, tabla));
            } else {
                verificarValor(columna, asignacion.valor(), d);
            }
        }

        if (u.where() != null) {
            verificarCondicion(u.where(), new Ambito(List.of(new TablaDeConsulta(u.tabla(), tabla))), d);
        } else {
            d.add(CodigoDiagnostico.A01.crear(u.update().linea(), u.update().columna(),
                    "UPDATE", "modificaría", u.tabla().lexema()));
        }
        return d;
    }

    private List<Diagnostico> verificarDelete(Eliminacion e, TablaSimbolos simbolos) {
        List<Diagnostico> d = new ArrayList<>();
        Tabla tabla = resolverTabla(e.tabla(), simbolos, d);
        if (e.where() != null) {
            verificarCondicion(e.where(), new Ambito(List.of(new TablaDeConsulta(e.tabla(), tabla))), d);
        } else {
            d.add(CodigoDiagnostico.A01.crear(e.delete().linea(), e.delete().columna(),
                    "DELETE", "eliminaría", e.tabla().lexema()));
        }
        return d;
    }

    // ================================================================== condiciones

    private void verificarCondicion(Condicion condicion, Ambito ambito, List<Diagnostico> d) {
        switch (condicion) {
            case Logica l -> {
                verificarCondicion(l.izquierda(), ambito, d);
                verificarCondicion(l.derecha(), ambito, d);
            }
            case Negacion n -> verificarCondicion(n.condicion(), ambito, d);
            case Comparacion c -> verificarComparacion(c, ambito, d);
            case EsNulo e -> resolverOperando(e.operando(), ambito, d);
        }
    }

    /**
     * Resuelve las dos columnas y contrasta sus tipos (M05). Comparar con NULL mediante = o &lt;&gt;
     * es A02 y no se contrasta, porque NULL es compatible con cualquier tipo.
     */
    private void verificarComparacion(Comparacion c, Ambito ambito, List<Diagnostico> d) {
        OperandoResuelto izquierdo = resolverOperando(c.izquierdo(), ambito, d);
        OperandoResuelto derecho = resolverOperando(c.derecho(), ambito, d);

        boolean conNulo = esLiteralNulo(c.izquierdo()) || esLiteralNulo(c.derecho());
        if (conNulo) {
            TipoToken operador = c.operador().tipo();
            if (operador == TipoToken.IGUAL || operador == TipoToken.DISTINTO) {
                d.add(CodigoDiagnostico.A02.crear(c.operador().linea(), c.operador().columna(),
                        c.operador().lexema()));
            }
            return;
        }
        if (izquierdo == null || derecho == null) {
            return;                                     // una columna no se resolvió: ya hay un error
        }

        Token inicio = c.izquierdo().primerToken();
        if (izquierdo.esColumna() && derecho.esColumna()) {
            if (!tipos.comparables(izquierdo.columna(), derecho.columna())) {
                d.add(incomparables(inicio, izquierdo, derecho));
            }
        } else if (izquierdo.esColumna() || derecho.esColumna()) {
            OperandoResuelto columna = izquierdo.esColumna() ? izquierdo : derecho;
            Literal valor = izquierdo.esColumna() ? derecho.literal() : izquierdo.literal();
            switch (tipos.evaluar(columna.columna(), valor, false)) {
                case TIPO_INCOMPATIBLE -> d.add(incomparables(inicio, izquierdo, derecho));
                case FECHA_INVALIDA -> d.add(CodigoDiagnostico.M05.crearVariante(inicio.linea(), inicio.columna(),
                        Variante.FECHA, valor.token().lexema(), columna.columna().nombre()));
                default -> { }
            }
        } else if (!tipos.comparables(izquierdo.literal(), derecho.literal())) {
            d.add(incomparables(inicio, izquierdo, derecho));
        }
    }

    private Diagnostico incomparables(Token inicio, OperandoResuelto izquierdo, OperandoResuelto derecho) {
        // Una columna a la izquierda lleva coma: «semestre», que es INT, con una cadena de texto.
        String texto = describir(izquierdo) + (izquierdo.esColumna() ? "," : "");
        return CodigoDiagnostico.M05.crear(inicio.linea(), inicio.columna(), texto, describir(derecho));
    }

    private OperandoResuelto resolverOperando(Operando operando, Ambito ambito, List<Diagnostico> d) {
        return switch (operando) {
            case ReferenciaColumna r -> {
                Columna columna = resolverColumna(r, ambito, d);
                yield columna == null ? null : new OperandoResuelto(r, columna);
            }
            case Literal l -> new OperandoResuelto(l, null);
        };
    }

    private static boolean esLiteralNulo(Operando operando) {
        return operando instanceof Literal literal && literal.esNulo();
    }

    // ================================================================== resolución de nombres

    /** M01: la tabla no existe. Devuelve null en ese caso, con la sugerencia más parecida si la hay. */
    private Tabla resolverTabla(Token nombre, TablaSimbolos simbolos, List<Diagnostico> d) {
        Optional<Tabla> tabla = simbolos.buscar(nombre.lexema());
        if (tabla.isPresent()) {
            return tabla.get();
        }
        Diagnostico error = CodigoDiagnostico.M01.crear(nombre.linea(), nombre.columna(), nombre.lexema());
        d.add(conSugerencia(error, sugeridor.sugerir(nombre.lexema(), simbolos.nombres())));
        return null;
    }

    /**
     * Busca la columna en las tablas de la sentencia. Devuelve null si no se puede resolver, y en ese
     * caso ya agregó el error: M04 (calificador que no es tabla de la consulta), M02 (no existe) o
     * M03 (existe en más de una tabla del JOIN y no está calificada).
     */
    private Columna resolverColumna(ReferenciaColumna referencia, Ambito ambito, List<Diagnostico> d) {
        Token nombre = referencia.nombre();

        if (referencia.calificador() != null) {
            Token calificador = referencia.calificador();
            Optional<TablaDeConsulta> encontrada = ambito.buscar(calificador.lexema());
            if (encontrada.isEmpty()) {
                d.add(CodigoDiagnostico.M04.crear(calificador.linea(), calificador.columna(), calificador.lexema()));
                return null;
            }
            Tabla tabla = encontrada.get().tabla();
            if (tabla == null) {
                return null;                            // la tabla no existe: ya se informó M01
            }
            Optional<Columna> columna = tabla.columna(nombre.lexema());
            if (columna.isEmpty()) {
                d.add(columnaInexistente(nombre, tabla));
            }
            return columna.orElse(null);
        }

        if (ambito.incompleto()) {
            return null;                                // no se sabe en qué tabla buscar
        }
        List<Tabla> coincidencias = ambito.existentes().stream()
                .filter(t -> t.columna(nombre.lexema()).isPresent())
                .toList();
        if (coincidencias.size() == 1) {
            return coincidencias.get(0).columna(nombre.lexema()).orElseThrow();
        }
        if (coincidencias.size() > 1) {
            String primera = coincidencias.get(0).nombre();
            d.add(CodigoDiagnostico.M03.crear(nombre.linea(), nombre.columna(), nombre.lexema(),
                    primera, coincidencias.get(1).nombre(), primera, nombre.lexema()));
            return null;
        }

        List<Tabla> tablas = ambito.existentes();
        Diagnostico error;
        if (tablas.size() == 1) {
            error = CodigoDiagnostico.M02.crear(nombre.linea(), nombre.columna(),
                    nombre.lexema(), tablas.get(0).nombre());
        } else {
            error = CodigoDiagnostico.M02.crearVariante(nombre.linea(), nombre.columna(),
                    Variante.EN_VARIAS_TABLAS, nombre.lexema(),
                    tablas.stream().map(Tabla::nombre).collect(Collectors.joining(", ")));
        }
        List<String> candidatas = tablas.stream().flatMap(t -> t.nombresDeColumnas().stream()).toList();
        d.add(conSugerencia(error, sugeridor.sugerir(nombre.lexema(), candidatas)));
        return null;
    }

    /** M02 para una columna escrita en una lista (INSERT, SET) o calificada, con sugerencia. */
    private Diagnostico columnaInexistente(Token nombre, Tabla tabla) {
        Diagnostico error = CodigoDiagnostico.M02.crear(nombre.linea(), nombre.columna(),
                nombre.lexema(), tabla.nombre());
        return conSugerencia(error, sugeridor.sugerir(nombre.lexema(), tabla.nombresDeColumnas()));
    }

    private static Diagnostico conSugerencia(Diagnostico error, Optional<String> sugerencia) {
        return sugerencia.map(error::conSugerencia).orElse(error);
    }

    // ================================================================== textos de los mensajes

    private static String minuscula(Token token) {
        return token.lexema().toLowerCase(Locale.ROOT);
    }

    private static String cantidad(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    /** Cómo se nombra un valor en un mensaje: «una cadena de texto», «un número»... */
    private static String describir(Literal valor) {
        return switch (valor.clase()) {
            case NUMERO -> "un número";
            case TEXTO -> "una cadena de texto";
            case LOGICO -> "un valor lógico";
            case NULO -> "NULL";
        };
    }

    private static String describir(OperandoResuelto operando) {
        if (!operando.esColumna()) {
            return describir(operando.literal());
        }
        ReferenciaColumna referencia = (ReferenciaColumna) operando.origen();
        return "«" + referencia.nombre().lexema() + "», que es " + operando.columna().tipoCompleto();
    }
}
