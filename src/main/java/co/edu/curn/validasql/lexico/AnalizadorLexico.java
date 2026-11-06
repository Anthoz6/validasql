package co.edu.curn.validasql.lexico;

import co.edu.curn.validasql.reporte.CodigoDiagnostico;
import co.edu.curn.validasql.reporte.Diagnostico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static co.edu.curn.validasql.lexico.TipoToken.ASTERISCO;
import static co.edu.curn.validasql.lexico.TipoToken.CADENA;
import static co.edu.curn.validasql.lexico.TipoToken.COMA;
import static co.edu.curn.validasql.lexico.TipoToken.DISTINTO;
import static co.edu.curn.validasql.lexico.TipoToken.IDENT;
import static co.edu.curn.validasql.lexico.TipoToken.IGUAL;
import static co.edu.curn.validasql.lexico.TipoToken.MAYOR;
import static co.edu.curn.validasql.lexico.TipoToken.MAYORIG;
import static co.edu.curn.validasql.lexico.TipoToken.MENOR;
import static co.edu.curn.validasql.lexico.TipoToken.MENORIG;
import static co.edu.curn.validasql.lexico.TipoToken.NUM_DEC;
import static co.edu.curn.validasql.lexico.TipoToken.NUM_ENT;
import static co.edu.curn.validasql.lexico.TipoToken.PAR_DER;
import static co.edu.curn.validasql.lexico.TipoToken.PAR_IZQ;
import static co.edu.curn.validasql.lexico.TipoToken.PUNTO;
import static co.edu.curn.validasql.lexico.TipoToken.PUNTOCOMA;

/**
 * Analizador léxico: el AFD de 19 estados (q0 a q18) del anexo, escrito a mano.
 *
 * <p>No guardamos el autómata como una matriz de transiciones. Cada grupo de estados es un método
 * que sigue leyendo mientras exista una transición, así que siempre se toma el lexema más largo:</p>
 * <ul>
 *   <li>q0: {@link #siguienteToken()}, un {@code if} sobre el primer carácter.</li>
 *   <li>q1: {@link #leerIdentificador}.</li>
 *   <li>q2, q3, q4: {@link #leerNumero}.</li>
 *   <li>q5, q6: {@link #leerCadena}.</li>
 *   <li>q7 a q11: {@link #leerOperador}.</li>
 *   <li>q12 a q18: el mapa {@link #SIMBOLOS}.</li>
 * </ul>
 *
 * <p>Línea y columna empiezan en 1. Los errores léxicos no detienen la lectura. Un objeto
 * guarda el estado de una lectura, así que se crea uno por cada texto que se analiza.</p>
 */
public final class AnalizadorLexico {

    /** Las 32 palabras reservadas, con la clave en minúscula porque el lenguaje no distingue mayúsculas. */
    private static final Map<String, TipoToken> PALABRAS_RESERVADAS = new HashMap<>();

    /** Símbolos de un solo carácter (estados q12 a q18, sin contar < y >, que tienen su propia rama). */
    private static final Map<Character, TipoToken> SIMBOLOS = Map.of(
            '=', IGUAL, ',', COMA, ';', PUNTOCOMA, '(', PAR_IZQ, ')', PAR_DER, '.', PUNTO, '*', ASTERISCO);

    static {
        for (TipoToken tipo : TipoToken.values()) {
            if (tipo.esPalabraReservada()) {
                PALABRAS_RESERVADAS.put(tipo.name().toLowerCase(Locale.ROOT), tipo);
            }
        }
    }

    private String texto = "";
    private int posicion;
    private int linea;
    private int columna;
    private List<Token> tokens;
    private List<Diagnostico> errores;

    public ResultadoLexico analizar(String entrada) {
        texto = entrada == null ? "" : entrada;
        posicion = 0;
        linea = 1;
        columna = 1;
        tokens = new ArrayList<>();
        errores = new ArrayList<>();

        while (saltarSeparadores()) {
            Token token = siguienteToken();
            if (token != null) {
                tokens.add(token);
            }
        }
        return new ResultadoLexico(texto, List.copyOf(tokens), List.copyOf(errores));
    }

    // ------------------------------------------------------------------ q0

    /**
     * Estado q0. El primer carácter decide la rama. Devuelve null cuando el lexema es un error
     * léxico (que ya quedó registrado) para que el ciclo siga con el carácter siguiente.
     */
    private Token siguienteToken() {
        int lineaInicio = linea;
        int columnaInicio = columna;
        char c = actual();

        if (esLetra(c)) {
            return leerIdentificador(lineaInicio, columnaInicio);
        }
        if (esDigito(c)) {
            return leerNumero(lineaInicio, columnaInicio);
        }
        if (c == '\'') {
            return leerCadena(lineaInicio, columnaInicio);
        }
        if (c == '<' || c == '>') {
            return leerOperador(lineaInicio, columnaInicio);
        }
        TipoToken simbolo = SIMBOLOS.get(c);
        if (simbolo != null) {
            int inicio = posicion;
            avanzar();                                  // q0 --símbolo--> q12..q18, que aceptan
            return crearToken(simbolo, inicio, lineaInicio, columnaInicio);
        }

        // q0 no tiene transición con este carácter: L01 y se descarta el carácter.
        int puntoDeCodigo = texto.codePointAt(posicion);
        String caracter = new String(Character.toChars(puntoDeCodigo));
        boolean letraConTilde = puntoDeCodigo > 127 && Character.isLetter(puntoDeCodigo);
        errores.add(CodigoDiagnostico.L01.crearVariante(lineaInicio, columnaInicio,
                letraConTilde ? CodigoDiagnostico.Variante.CON_TILDES : CodigoDiagnostico.Variante.BASE,
                caracter));
        for (int i = 0; i < Character.charCount(puntoDeCodigo); i++) {
            avanzar();
        }
        return null;
    }

    // ------------------------------------------------------------------ q1

    /** q1: letra (letra | dígito)*. Al aceptar se busca el lexema en la tabla de palabras reservadas. */
    private Token leerIdentificador(int lineaInicio, int columnaInicio) {
        int inicio = posicion;
        avanzar();                                      // q0 --L--> q1
        while (esLetra(actual()) || esDigito(actual())) {
            avanzar();                                  // q1 --L,D--> q1
        }
        String lexema = texto.substring(inicio, posicion);
        TipoToken reservada = PALABRAS_RESERVADAS.get(lexema.toLowerCase(Locale.ROOT));
        return new Token(reservada != null ? reservada : IDENT, lexema, lineaInicio, columnaInicio);
    }

    // ------------------------------------------------------------------ q2, q3, q4

    /** q2 → q3 → q4 del AFD. Recibe la línea y la columna del primer dígito. */
    private Token leerNumero(int lineaInicio, int columnaInicio) {
        int inicio = posicion;
        while (esDigito(actual())) {
            avanzar();                                  // q0 --D--> q2, q2 --D--> q2
        }
        if (actual() != '.') {
            return crearToken(NUM_ENT, inicio, lineaInicio, columnaInicio);     // q2 acepta
        }
        avanzar();                                      // q2 --.--> q3
        if (!esDigito(actual())) {                      // q3 no es de aceptación
            errores.add(CodigoDiagnostico.L03.crear(lineaInicio, columnaInicio));
            return null;
        }
        while (esDigito(actual())) {
            avanzar();                                  // q3 --D--> q4, q4 --D--> q4
        }
        return crearToken(NUM_DEC, inicio, lineaInicio, columnaInicio);         // q4 acepta
    }

    // ------------------------------------------------------------------ q5, q6

    /**
     * q5 → q6. Dentro de la cadena vale cualquier carácter salvo la comilla y el salto de línea.
     * Si llega al salto de línea o al final del texto, q5 no acepta y se informa L02. Ese error
     * también le sirve al segmentador para cerrar la sentencia al final de la línea.
     */
    private Token leerCadena(int lineaInicio, int columnaInicio) {
        int inicio = posicion;
        avanzar();                                      // q0 --'--> q5
        while (!fin() && actual() != '\'' && actual() != '\n' && actual() != '\r') {
            avanzar();                                  // q5 --otro--> q5
        }
        if (fin() || actual() != '\'') {
            errores.add(CodigoDiagnostico.L02.crear(lineaInicio, columnaInicio));
            return null;
        }
        avanzar();                                      // q5 --'--> q6, que acepta
        return crearToken(CADENA, inicio, lineaInicio, columnaInicio);
    }

    // ------------------------------------------------------------------ q7 a q11

    /** q7 a q11: {@code <}, {@code <=}, {@code <>}, {@code >} y {@code >=}. Mira un carácter adelante. */
    private Token leerOperador(int lineaInicio, int columnaInicio) {
        int inicio = posicion;
        char primero = actual();
        avanzar();                                      // q0 --<--> q7  o  q0 --> --> q10
        TipoToken tipo;
        if (primero == '<') {
            if (actual() == '=') {
                avanzar();                              // q7 --=--> q8
                tipo = MENORIG;
            } else if (actual() == '>') {
                avanzar();                              // q7 --> --> q9
                tipo = DISTINTO;
            } else {
                tipo = MENOR;                           // q7 acepta
            }
        } else if (actual() == '=') {
            avanzar();                                  // q10 --=--> q11
            tipo = MAYORIG;
        } else {
            tipo = MAYOR;                               // q10 acepta
        }
        return crearToken(tipo, inicio, lineaInicio, columnaInicio);
    }

    // ------------------------------------------------------------------ utilidades

    /** Avanza sobre espacios, tabulaciones y saltos de línea. Devuelve false al llegar al final. */
    private boolean saltarSeparadores() {
        while (!fin() && (actual() == ' ' || actual() == '\t' || actual() == '\n' || actual() == '\r')) {
            avanzar();
        }
        return !fin();
    }

    private Token crearToken(TipoToken tipo, int inicio, int lineaInicio, int columnaInicio) {
        return new Token(tipo, texto.substring(inicio, posicion), lineaInicio, columnaInicio);
    }

    private boolean fin() {
        return posicion >= texto.length();
    }

    /** Carácter actual, o '\0' si ya se terminó el texto (que no pertenece a ninguna transición). */
    private char actual() {
        return fin() ? '\0' : texto.charAt(posicion);
    }

    /** Consume un carácter y actualiza línea y columna. */
    private void avanzar() {
        char c = texto.charAt(posicion++);
        if (c == '\n') {
            linea++;
            columna = 1;
        } else {
            columna++;
        }
    }

    private static boolean esLetra(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean esDigito(char c) {
        return c >= '0' && c <= '9';
    }
}
