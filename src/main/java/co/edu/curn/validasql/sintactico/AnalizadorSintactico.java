package co.edu.curn.validasql.sintactico;

import co.edu.curn.validasql.arbol.Actualizacion;
import co.edu.curn.validasql.arbol.Asignacion;
import co.edu.curn.validasql.arbol.Comparacion;
import co.edu.curn.validasql.arbol.Condicion;
import co.edu.curn.validasql.arbol.CrearTabla;
import co.edu.curn.validasql.arbol.DefColumna;
import co.edu.curn.validasql.arbol.Eliminacion;
import co.edu.curn.validasql.arbol.EsNulo;
import co.edu.curn.validasql.arbol.Insercion;
import co.edu.curn.validasql.arbol.Join;
import co.edu.curn.validasql.arbol.Literal;
import co.edu.curn.validasql.arbol.Logica;
import co.edu.curn.validasql.arbol.Negacion;
import co.edu.curn.validasql.arbol.Operando;
import co.edu.curn.validasql.arbol.Orden;
import co.edu.curn.validasql.arbol.ReferenciaColumna;
import co.edu.curn.validasql.arbol.Seleccion;
import co.edu.curn.validasql.arbol.Sentencia;
import co.edu.curn.validasql.arbol.TipoDato;
import co.edu.curn.validasql.lexico.TipoToken;
import co.edu.curn.validasql.lexico.Token;
import co.edu.curn.validasql.lexico.UnidadSentencia;
import co.edu.curn.validasql.reporte.CodigoDiagnostico;
import co.edu.curn.validasql.reporte.Diagnostico;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static co.edu.curn.validasql.lexico.TipoToken.AND;
import static co.edu.curn.validasql.lexico.TipoToken.ASC;
import static co.edu.curn.validasql.lexico.TipoToken.ASTERISCO;
import static co.edu.curn.validasql.lexico.TipoToken.BOOLEAN;
import static co.edu.curn.validasql.lexico.TipoToken.BY;
import static co.edu.curn.validasql.lexico.TipoToken.CADENA;
import static co.edu.curn.validasql.lexico.TipoToken.COMA;
import static co.edu.curn.validasql.lexico.TipoToken.CREATE;
import static co.edu.curn.validasql.lexico.TipoToken.DATE;
import static co.edu.curn.validasql.lexico.TipoToken.DECIMAL;
import static co.edu.curn.validasql.lexico.TipoToken.DELETE;
import static co.edu.curn.validasql.lexico.TipoToken.DESC;
import static co.edu.curn.validasql.lexico.TipoToken.DISTINTO;
import static co.edu.curn.validasql.lexico.TipoToken.FALSE;
import static co.edu.curn.validasql.lexico.TipoToken.FROM;
import static co.edu.curn.validasql.lexico.TipoToken.IDENT;
import static co.edu.curn.validasql.lexico.TipoToken.IGUAL;
import static co.edu.curn.validasql.lexico.TipoToken.INNER;
import static co.edu.curn.validasql.lexico.TipoToken.INSERT;
import static co.edu.curn.validasql.lexico.TipoToken.INT;
import static co.edu.curn.validasql.lexico.TipoToken.INTO;
import static co.edu.curn.validasql.lexico.TipoToken.IS;
import static co.edu.curn.validasql.lexico.TipoToken.JOIN;
import static co.edu.curn.validasql.lexico.TipoToken.KEY;
import static co.edu.curn.validasql.lexico.TipoToken.MAYOR;
import static co.edu.curn.validasql.lexico.TipoToken.MAYORIG;
import static co.edu.curn.validasql.lexico.TipoToken.MENOR;
import static co.edu.curn.validasql.lexico.TipoToken.MENORIG;
import static co.edu.curn.validasql.lexico.TipoToken.NOT;
import static co.edu.curn.validasql.lexico.TipoToken.NULL;
import static co.edu.curn.validasql.lexico.TipoToken.NUM_DEC;
import static co.edu.curn.validasql.lexico.TipoToken.NUM_ENT;
import static co.edu.curn.validasql.lexico.TipoToken.ON;
import static co.edu.curn.validasql.lexico.TipoToken.OR;
import static co.edu.curn.validasql.lexico.TipoToken.ORDER;
import static co.edu.curn.validasql.lexico.TipoToken.PAR_DER;
import static co.edu.curn.validasql.lexico.TipoToken.PAR_IZQ;
import static co.edu.curn.validasql.lexico.TipoToken.PRIMARY;
import static co.edu.curn.validasql.lexico.TipoToken.PUNTO;
import static co.edu.curn.validasql.lexico.TipoToken.PUNTOCOMA;
import static co.edu.curn.validasql.lexico.TipoToken.SELECT;
import static co.edu.curn.validasql.lexico.TipoToken.SET;
import static co.edu.curn.validasql.lexico.TipoToken.TABLE;
import static co.edu.curn.validasql.lexico.TipoToken.TRUE;
import static co.edu.curn.validasql.lexico.TipoToken.UPDATE;
import static co.edu.curn.validasql.lexico.TipoToken.VALUES;
import static co.edu.curn.validasql.lexico.TipoToken.VARCHAR;
import static co.edu.curn.validasql.lexico.TipoToken.WHERE;

/**
 * Analizador sintáctico descendente recursivo para la gramática LL(1) del anexo.
 *
 * <p>Cada no terminal con cuerpo propio es un método con su nombre en minúscula ({@code select()},
 * {@code cond()}, {@code prim()}...): son 22. Las colas de las listas (DefColR, ColsR, IdR, LitR,
 * AsigR, CondO, CondYR) y Restr son un {@code while} dentro del método padre, y ColQ, Dir, PrimR e
 * IsR son un {@code if}. Prog no está aquí: es el ciclo de {@code ServicioAnalisis} sobre las
 * sentencias que entrega el segmentador.</p>
 *
 * <p>Para que el mensaje S01 nombre solo los tokens que podían aparecer en ese punto, el parser usa
 * cuatro auxiliares: {@link #es} pregunta por el token actual y lo agrega a los esperados;
 * {@link #consumir} avanza y vacía los esperados, o lanza S01 con ellos; {@link #decidir} anota en
 * la traza la producción elegida por PRIMERO; y {@link #epsilon} anota una producción vacía, por
 * SIGUIENTE o por DESCARTE. Con DESCARTE el error no se lanza ahí sino en el siguiente
 * {@code consumir}.</p>
 *
 * <p>Se detiene en el primer error de la sentencia. Guarda el estado de una lectura, así que se crea
 * uno por cada sentencia.</p>
 */
public final class AnalizadorSintactico {

    /** Se lanza al primer error para abandonar la sentencia. No guarda traza de pila: es control de flujo. */
    private static final class ErrorSintactico extends RuntimeException {
        private final transient Diagnostico diagnostico;

        ErrorSintactico(Diagnostico diagnostico) {
            super(diagnostico.mensaje(), null, false, false);
            this.diagnostico = diagnostico;
        }
    }

    /** Resultado de {@link #cols()}: o un asterisco, o una lista de columnas. */
    private record Columnas(Token asterisco, List<ReferenciaColumna> columnas) {
    }

    /** Resultado de {@link #tipo()}: el tipo y, para VARCHAR, su longitud. */
    private record TipoLeido(TipoDato tipo, int longitud) {
    }

    private static final ConjuntosLL1 CONJUNTOS = ConjuntosLL1.gramatica();

    private List<Token> tokens;
    private int posicion;
    private int lineaInicial;
    private int columnaInicial;
    private final Set<TipoToken> esperados = EnumSet.noneOf(TipoToken.class);
    private final List<PasoTraza> traza = new ArrayList<>();

    public ResultadoSintactico analizar(UnidadSentencia unidad) {
        tokens = unidad.tokens();
        posicion = 0;
        lineaInicial = unidad.linea();
        columnaInicial = unidad.columna();
        esperados.clear();
        traza.clear();
        try {
            Sentencia arbol = sent();
            return new ResultadoSintactico(arbol, List.copyOf(traza), null);
        } catch (ErrorSintactico e) {
            return new ResultadoSintactico(null, List.copyOf(traza), e.diagnostico);
        }
    }

    // ================================================================== sentencias

    /** Sent → Crear ';' | Select ';' | Insert ';' | Update ';' | Delete ';' */
    private Sentencia sent() {
        Sentencia sentencia;
        if (es(CREATE)) {
            decidir("Sent", "Sent → Crear ';'");
            sentencia = crear();
        } else if (es(SELECT)) {
            decidir("Sent", "Sent → Select ';'");
            sentencia = select();
        } else if (es(INSERT)) {
            decidir("Sent", "Sent → Insert ';'");
            sentencia = insert();
        } else if (es(UPDATE)) {
            decidir("Sent", "Sent → Update ';'");
            sentencia = update();
        } else if (es(DELETE)) {
            decidir("Sent", "Sent → Delete ';'");
            sentencia = delete();
        } else {
            throw error();
        }
        consumir(PUNTOCOMA);
        return sentencia;
    }

    // ------------------------------------------------------------------ CREATE TABLE

    /** Crear → CREATE TABLE IDENT '(' DefCol DefColR ')'      DefColR → ',' DefCol DefColR | ε */
    private CrearTabla crear() {
        decidir("Crear", "Crear → CREATE TABLE IDENT '(' DefCol DefColR ')'");
        consumir(CREATE);
        consumir(TABLE);
        Token nombre = consumir(IDENT);
        consumir(PAR_IZQ);
        List<DefColumna> columnas = new ArrayList<>();
        columnas.add(defCol());
        while (es(COMA)) {                              // DefColR → ',' DefCol DefColR
            decidir("DefColR", "DefColR → ',' DefCol DefColR");
            consumir(COMA);
            columnas.add(defCol());
        }
        epsilon("DefColR");
        consumir(PAR_DER);
        return new CrearTabla(nombre, columnas);
    }

    /** DefCol → IDENT Tipo Restr      Restr → PRIMARY KEY Restr | NOT NULL Restr | ε */
    private DefColumna defCol() {
        decidir("DefCol", "DefCol → IDENT Tipo Restr");
        Token nombre = consumir(IDENT);
        TipoLeido tipo = tipo();
        boolean notNull = false;
        boolean clavePrimaria = false;
        while (es(PRIMARY) || es(NOT)) {                // Restr
            if (es(PRIMARY)) {
                decidir("Restr", "Restr → PRIMARY KEY Restr");
                consumir(PRIMARY);
                consumir(KEY);
                clavePrimaria = true;
            } else {
                decidir("Restr", "Restr → NOT NULL Restr");
                consumir(NOT);
                consumir(NULL);
                notNull = true;
            }
        }
        epsilon("Restr");
        return new DefColumna(nombre, tipo.tipo(), tipo.longitud(), notNull, clavePrimaria);
    }

    /** Tipo → INT | DECIMAL | VARCHAR '(' NUM_ENT ')' | DATE | BOOLEAN */
    private TipoLeido tipo() {
        if (es(INT)) {
            decidir("Tipo", "Tipo → INT");
            consumir(INT);
            return new TipoLeido(TipoDato.INT, 0);
        } else if (es(DECIMAL)) {
            decidir("Tipo", "Tipo → DECIMAL");
            consumir(DECIMAL);
            return new TipoLeido(TipoDato.DECIMAL, 0);
        } else if (es(VARCHAR)) {
            decidir("Tipo", "Tipo → VARCHAR '(' NUM_ENT ')'");
            consumir(VARCHAR);
            consumir(PAR_IZQ);
            Token longitud = consumir(NUM_ENT);
            consumir(PAR_DER);
            return new TipoLeido(TipoDato.VARCHAR, aEntero(longitud));
        } else if (es(DATE)) {
            decidir("Tipo", "Tipo → DATE");
            consumir(DATE);
            return new TipoLeido(TipoDato.DATE, 0);
        } else if (es(BOOLEAN)) {
            decidir("Tipo", "Tipo → BOOLEAN");
            consumir(BOOLEAN);
            return new TipoLeido(TipoDato.BOOLEAN, 0);
        }
        throw error();
    }

    // ------------------------------------------------------------------ SELECT

    /** Select → SELECT Cols FROM IDENT Join Where Orden */
    private Seleccion select() {
        decidir("Select", "Select → SELECT Cols FROM IDENT Join Where Orden");
        consumir(SELECT);
        Columnas columnas = cols();
        consumir(FROM);
        Token tabla = consumir(IDENT);
        Join join = join();
        Condicion where = where();
        Orden orden = orden();
        return new Seleccion(columnas.asterisco(), columnas.columnas(), tabla, join, where, orden);
    }

    /** Cols → '*' | Col ColsR        ColsR → ',' Col ColsR | ε */
    private Columnas cols() {
        if (es(ASTERISCO)) {
            decidir("Cols", "Cols → '*'");
            return new Columnas(consumir(ASTERISCO), List.of());
        }
        decidir("Cols", "Cols → Col ColsR");
        List<ReferenciaColumna> columnas = new ArrayList<>();
        columnas.add(col());
        while (es(COMA)) {                              // ColsR → ',' Col ColsR
            decidir("ColsR", "ColsR → ',' Col ColsR");
            consumir(COMA);
            columnas.add(col());
        }
        epsilon("ColsR");                               // ColsR → ε, por SIGUIENTE o por DESCARTE
        return new Columnas(null, columnas);
    }

    /** Col → IDENT ColQ        ColQ → '.' IDENT | ε */
    private ReferenciaColumna col() {
        decidir("Col", "Col → IDENT ColQ");
        Token primero = consumir(IDENT);
        if (es(PUNTO)) {                                // ColQ → '.' IDENT
            decidir("ColQ", "ColQ → '.' IDENT");
            consumir(PUNTO);
            Token segundo = consumir(IDENT);
            return new ReferenciaColumna(primero, segundo);
        }
        epsilon("ColQ");
        return new ReferenciaColumna(null, primero);
    }

    /** Join → INNER JOIN IDENT ON Cond | ε */
    private Join join() {
        if (es(INNER)) {
            decidir("Join", "Join → INNER JOIN IDENT ON Cond");
            consumir(INNER);
            consumir(JOIN);
            Token tabla = consumir(IDENT);
            consumir(ON);
            return new Join(tabla, cond());
        }
        epsilon("Join");
        return null;
    }

    /** Where → WHERE Cond | ε */
    private Condicion where() {
        if (es(WHERE)) {
            decidir("Where", "Where → WHERE Cond");
            consumir(WHERE);
            return cond();
        }
        epsilon("Where");
        return null;
    }

    /** Orden → ORDER BY Col Dir | ε        Dir → ASC | DESC | ε */
    private Orden orden() {
        if (es(ORDER)) {
            decidir("Orden", "Orden → ORDER BY Col Dir");
            consumir(ORDER);
            consumir(BY);
            ReferenciaColumna columna = col();
            Token direccion = null;
            if (es(ASC)) {                              // Dir
                decidir("Dir", "Dir → ASC");
                direccion = consumir(ASC);
            } else if (es(DESC)) {
                decidir("Dir", "Dir → DESC");
                direccion = consumir(DESC);
            } else {
                epsilon("Dir");
            }
            return new Orden(columna, direccion);
        }
        epsilon("Orden");
        return null;
    }

    // ------------------------------------------------------------------ INSERT

    /** Insert → INSERT INTO IDENT InsCols VALUES '(' Lit LitR ')'      LitR → ',' Lit LitR | ε */
    private Insercion insert() {
        decidir("Insert", "Insert → INSERT INTO IDENT InsCols VALUES '(' Lit LitR ')'");
        consumir(INSERT);
        consumir(INTO);
        Token tabla = consumir(IDENT);
        List<Token> columnas = insCols();
        Token values = consumir(VALUES);
        consumir(PAR_IZQ);
        List<Literal> valores = new ArrayList<>();
        valores.add(lit());
        while (es(COMA)) {                              // LitR → ',' Lit LitR
            decidir("LitR", "LitR → ',' Lit LitR");
            consumir(COMA);
            valores.add(lit());
        }
        epsilon("LitR");
        consumir(PAR_DER);
        return new Insercion(tabla, columnas, values, valores);
    }

    /** InsCols → '(' IDENT IdR ')' | ε        IdR → ',' IDENT IdR | ε. Devuelve null si es ε. */
    private List<Token> insCols() {
        if (es(PAR_IZQ)) {
            decidir("InsCols", "InsCols → '(' IDENT IdR ')'");
            consumir(PAR_IZQ);
            List<Token> columnas = new ArrayList<>();
            columnas.add(consumir(IDENT));
            while (es(COMA)) {                          // IdR → ',' IDENT IdR
                decidir("IdR", "IdR → ',' IDENT IdR");
                consumir(COMA);
                columnas.add(consumir(IDENT));
            }
            epsilon("IdR");
            consumir(PAR_DER);
            return columnas;
        }
        epsilon("InsCols");
        return null;
    }

    // ------------------------------------------------------------------ UPDATE y DELETE

    /** Update → UPDATE IDENT SET Asig AsigR Where      AsigR → ',' Asig AsigR | ε */
    private Actualizacion update() {
        decidir("Update", "Update → UPDATE IDENT SET Asig AsigR Where");
        Token update = consumir(UPDATE);
        Token tabla = consumir(IDENT);
        consumir(SET);
        List<Asignacion> asignaciones = new ArrayList<>();
        asignaciones.add(asig());
        while (es(COMA)) {                              // AsigR → ',' Asig AsigR
            decidir("AsigR", "AsigR → ',' Asig AsigR");
            consumir(COMA);
            asignaciones.add(asig());
        }
        epsilon("AsigR");
        Condicion where = where();
        return new Actualizacion(update, tabla, asignaciones, where);
    }

    /** Asig → IDENT '=' Lit */
    private Asignacion asig() {
        decidir("Asig", "Asig → IDENT '=' Lit");
        Token columna = consumir(IDENT);
        consumir(IGUAL);
        return new Asignacion(columna, lit());
    }

    /** Delete → DELETE FROM IDENT Where */
    private Eliminacion delete() {
        decidir("Delete", "Delete → DELETE FROM IDENT Where");
        Token delete = consumir(DELETE);
        consumir(FROM);
        Token tabla = consumir(IDENT);
        return new Eliminacion(delete, tabla, where());
    }

    // ================================================================== condiciones
    // OR < AND < NOT en precedencia. Las colas CondO y CondYR son ciclos, así que AND y OR
    // asocian a la izquierda: a OR b OR c es (a OR b) OR c.

    /** Cond → CondY CondO        CondO → OR CondY CondO | ε */
    private Condicion cond() {
        decidir("Cond", "Cond → CondY CondO");
        Condicion resultado = condY();
        while (es(OR)) {
            decidir("CondO", "CondO → OR CondY CondO");
            Token operador = consumir(OR);
            resultado = new Logica(operador, resultado, condY());
        }
        epsilon("CondO");
        return resultado;
    }

    /** CondY → CondN CondYR        CondYR → AND CondN CondYR | ε */
    private Condicion condY() {
        decidir("CondY", "CondY → CondN CondYR");
        Condicion resultado = condN();
        while (es(AND)) {
            decidir("CondYR", "CondYR → AND CondN CondYR");
            Token operador = consumir(AND);
            resultado = new Logica(operador, resultado, condN());
        }
        epsilon("CondYR");
        return resultado;
    }

    /** CondN → NOT CondN | Prim */
    private Condicion condN() {
        if (es(NOT)) {
            decidir("CondN", "CondN → NOT CondN");
            Token not = consumir(NOT);
            return new Negacion(not, condN());
        }
        if (!esInicioDePrim()) {
            throw error();
        }
        decidir("CondN", "CondN → Prim");
        return prim();
    }

    /**
     * Prim → '(' Cond ')' | Opnd PrimR
     * PrimR → OpRel Opnd | IS IsR        IsR → NULL | NOT NULL
     */
    private Condicion prim() {
        if (es(PAR_IZQ)) {
            decidir("Prim", "Prim → '(' Cond ')'");
            consumir(PAR_IZQ);
            Condicion interna = cond();
            consumir(PAR_DER);
            return interna;
        }
        decidir("Prim", "Prim → Opnd PrimR");
        Operando izquierdo = opnd();
        if (esOperadorRelacional()) {                   // PrimR → OpRel Opnd
            decidir("PrimR", "PrimR → OpRel Opnd");
            Token operador = opRel();
            return new Comparacion(izquierdo, operador, opnd());
        }
        if (es(IS)) {                                   // PrimR → IS IsR
            decidir("PrimR", "PrimR → IS IsR");
            Token is = consumir(IS);
            if (es(NULL)) {                             // IsR → NULL
                decidir("IsR", "IsR → NULL");
                consumir(NULL);
                return new EsNulo(izquierdo, is, false);
            }
            if (es(NOT)) {                              // IsR → NOT NULL
                decidir("IsR", "IsR → NOT NULL");
                consumir(NOT);
                consumir(NULL);
                return new EsNulo(izquierdo, is, true);
            }
        }
        throw error();
    }

    /** Opnd → Col | Lit */
    private Operando opnd() {
        if (es(IDENT)) {
            decidir("Opnd", "Opnd → Col");
            return col();
        }
        if (esInicioDeLit()) {
            decidir("Opnd", "Opnd → Lit");
            return lit();
        }
        throw error();
    }

    /** OpRel → '=' | '<>' | '<' | '>' | '<=' | '>=' */
    private Token opRel() {
        if (!esOperadorRelacional()) {
            throw error();
        }
        TipoToken tipo = actual().tipo();
        decidir("OpRel", "OpRel → " + tipo.descripcion());
        return consumir(tipo);
    }

    /** Lit → NUM_ENT | NUM_DEC | CADENA | TRUE | FALSE | NULL */
    private Literal lit() {
        if (!esInicioDeLit()) {
            throw error();
        }
        TipoToken tipo = actual().tipo();
        decidir("Lit", "Lit → " + tipo.name());
        return new Literal(consumir(tipo));
    }

    // ================================================================== PRIMERO de los no terminales con varias salidas

    private boolean esOperadorRelacional() {
        return es(IGUAL) | es(DISTINTO) | es(MENOR) | es(MAYOR) | es(MENORIG) | es(MAYORIG);
    }

    private boolean esInicioDeLit() {
        return es(NUM_ENT) | es(NUM_DEC) | es(CADENA) | es(TRUE) | es(FALSE) | es(NULL);
    }

    /** PRIMERO(Prim) = '(' más PRIMERO(Opnd). */
    private boolean esInicioDePrim() {
        return es(PAR_IZQ) | es(IDENT) | esInicioDeLit();
    }

    // ================================================================== auxiliares del parser

    /**
     * Pregunta si el token actual es de tipo {@code tipo} y agrega {@code tipo} a los esperados,
     * aunque la respuesta sea sí: {@link #consumir} los vacía al avanzar. Los operadores de
     * arriba usan {@code |} y no {@code ||} para que se agreguen todos los alternativos.
     */
    private boolean es(TipoToken tipo) {
        esperados.add(tipo);
        Token token = actual();
        return token != null && token.tipo() == tipo;
    }

    /** Avanza si el token coincide y vacía los esperados; si no, lanza S01 con los esperados. */
    private Token consumir(TipoToken tipo) {
        esperados.add(tipo);
        Token token = actual();
        if (token == null || token.tipo() != tipo) {
            throw error();
        }
        posicion++;
        esperados.clear();
        return token;
    }

    /** Anota la producción elegida porque el token actual está en su PRIMERO. */
    private void decidir(String noTerminal, String produccion) {
        registrar(noTerminal, produccion, Motivo.PRIMERO);
    }

    /**
     * Anota la producción vacía del no terminal. Si el token actual está en SIGUIENTE el motivo es
     * SIGUIENTE; si no, DESCARTE, y el error aparecerá en el siguiente {@code consumir}.
     */
    private void epsilon(String noTerminal) {
        Token token = actual();
        boolean enSiguiente = token != null && CONJUNTOS.siguiente(noTerminal).contains(token.tipo().name());
        registrar(noTerminal, noTerminal + " → ε", enSiguiente ? Motivo.SIGUIENTE : Motivo.DESCARTE);
    }

    private void registrar(String noTerminal, String produccion, Motivo motivo) {
        Token token = actual();
        traza.add(new PasoTraza(traza.size() + 1, noTerminal, token == null ? "(fin)" : token.lexema(),
                produccion, motivo));
    }

    private Token actual() {
        return posicion < tokens.size() ? tokens.get(posicion) : null;
    }

    private static int aEntero(Token numero) {
        try {
            return Integer.parseInt(numero.lexema());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;           // un entero gigante: ninguna cadena lo alcanza
        }
    }

    // ================================================================== error S01

    /**
     * Arma el S01 con lo que se esperaba y lo que llegó. Si ya no quedan tokens, se ubica justo
     * después del último token de la sentencia.
     */
    private ErrorSintactico error() {
        Token encontrado = actual();
        int linea;
        int columna;
        if (encontrado != null) {
            linea = encontrado.linea();
            columna = encontrado.columna();
        } else if (!tokens.isEmpty()) {
            Token ultimo = tokens.get(tokens.size() - 1);
            linea = ultimo.linea();
            columna = ultimo.columnaFinal();
        } else {
            linea = lineaInicial;
            columna = columnaInicial;
        }
        List<String> nombres = esperados.stream().map(TipoToken::descripcion).toList();
        return new ErrorSintactico(CodigoDiagnostico.S01.crear(linea, columna,
                enumerar(nombres), describir(encontrado)));
    }

    /** «A», «A o B», «A, B o C». Se usa «u» delante de una palabra que empieza con o (OR, ON, ORDER). */
    private static String enumerar(List<String> elementos) {
        if (elementos.size() == 1) {
            return elementos.get(0);
        }
        String ultimo = elementos.get(elementos.size() - 1);
        String conjuncion = ultimo.startsWith("O") ? " u " : " o ";
        return String.join(", ", elementos.subList(0, elementos.size() - 1)) + conjuncion + ultimo;
    }

    private static String describir(Token token) {
        if (token == null) {
            return "el final de la sentencia";
        }
        return switch (token.tipo()) {
            case IDENT -> "el identificador «" + token.lexema() + "»";
            case NUM_ENT, NUM_DEC -> "el número «" + token.lexema() + "»";
            case CADENA -> "la cadena " + token.lexema();
            default -> token.tipo().esPalabraReservada()
                    ? "la palabra reservada «" + token.lexema().toUpperCase() + "»"
                    : "el símbolo «" + token.lexema() + "»";
        };
    }
}
