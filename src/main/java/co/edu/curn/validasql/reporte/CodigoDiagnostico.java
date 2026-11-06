package co.edu.curn.validasql.reporte;

import java.util.Locale;

import static co.edu.curn.validasql.reporte.Severidad.ADVERTENCIA;
import static co.edu.curn.validasql.reporte.Severidad.ERROR_LEXICO;
import static co.edu.curn.validasql.reporte.Severidad.ERROR_SEMANTICO;
import static co.edu.curn.validasql.reporte.Severidad.ERROR_SINTACTICO;

/**
 * Catálogo de los 15 códigos de error y advertencia del anexo técnico.
 *
 * <p>Es el único lugar donde se escriben los mensajes. Cada código tiene una severidad y una o
 * más plantillas: la primera es la variante por defecto y las demás cubren casos del mismo código
 * que cambian la redacción (por ejemplo, M05 se redacta distinto para una comparación que para
 * un texto demasiado largo). Las plantillas usan {@link String#format}.</p>
 */
public enum CodigoDiagnostico {

    // Léxicos
    L01(ERROR_LEXICO,
            "Carácter no válido «%s»: no pertenece al alfabeto del lenguaje.",
            "Carácter no válido «%s»: los nombres de tablas y columnas no pueden llevar tildes ni ñ."),
    L02(ERROR_LEXICO,
            "Cadena sin cerrar: falta la comilla simple de cierre antes del fin de la línea."),
    L03(ERROR_LEXICO,
            "Número decimal incompleto: después del punto se esperaba un dígito."),

    // Sintáctico
    S01(ERROR_SINTACTICO,
            "Se esperaba %s, pero se encontró %s."),

    // Semánticos
    M01(ERROR_SEMANTICO,
            "La tabla «%s» no existe."),
    M02(ERROR_SEMANTICO,
            "La columna «%s» no existe en la tabla «%s».",
            "La columna «%s» no existe en ninguna de las tablas de la consulta (%s)."),
    M03(ERROR_SEMANTICO,
            "La columna «%s» es ambigua: existe en «%s» y «%s». Califícala, por ejemplo «%s.%s»."),
    M04(ERROR_SEMANTICO,
            "«%s» no es una tabla de esta consulta."),
    M05(ERROR_SEMANTICO,
            "No se puede comparar %s con %s.",
            "No se puede asignar %s a la columna «%s», que es %s.",
            "El texto tiene %d caracteres y la columna «%s» admite como máximo %d.",
            "El valor %s no es una fecha real con formato 'AAAA-MM-DD' para la columna «%s», que es DATE."),
    M06(ERROR_SEMANTICO,
            "La sentencia nombra %s pero entrega %s.",
            "La tabla «%s» tiene %s pero la sentencia entrega %s."),
    M07(ERROR_SEMANTICO,
            "Falta un valor para la columna «%s», que es NOT NULL.",
            "La columna «%s» es NOT NULL y no puede recibir NULL."),
    M08(ERROR_SEMANTICO,
            "La tabla «%s» ya existe.",
            "La columna «%s» está repetida en la tabla «%s».",
            "La tabla «%s» ya tiene la clave primaria «%s»; solo se admite una.",
            "La columna «%s» se asigna más de una vez.",
            "La columna «%s» aparece más de una vez en la lista de columnas."),

    // Advertencias
    A01(ADVERTENCIA,
            "La sentencia %s no tiene WHERE, así que %s todos los registros de «%s»."),
    A02(ADVERTENCIA,
            "Comparar con NULL usando «%s» nunca es verdadero. Usa IS NULL o IS NOT NULL."),
    A03(ADVERTENCIA,
            "SELECT * devuelve todas las columnas; conviene listar solo las necesarias.");

    /** Números de plantilla de cada código. La variante por defecto siempre es {@link #BASE}. */
    public static final class Variante {
        public static final int BASE = 0;

        /** L01: la letra tiene tilde o es ñ. */
        public static final int CON_TILDES = 1;
        /** M02: la columna se busca en varias tablas (consulta con JOIN). */
        public static final int EN_VARIAS_TABLAS = 1;
        /** M05: asignación en INSERT o UPDATE. */
        public static final int ASIGNACION = 1;
        /** M05: texto más largo que VARCHAR(n). */
        public static final int LONGITUD = 2;
        /** M05: cadena que no es una fecha real. */
        public static final int FECHA = 3;
        /** M06: INSERT con lista de columnas. */
        public static final int CON_LISTA = 0;
        /** M06: INSERT sin lista de columnas. */
        public static final int SIN_LISTA = 1;
        /** M07: falta un valor para una columna NOT NULL. */
        public static final int FALTA_VALOR = 0;
        /** M07: se intenta guardar NULL en una columna NOT NULL. */
        public static final int NULO_EN_NOT_NULL = 1;
        /** M08: CREATE TABLE con un nombre de tabla que ya existe. */
        public static final int TABLA_EXISTE = 0;
        public static final int COLUMNA_REPETIDA = 1;
        public static final int CLAVE_REPETIDA = 2;
        /** M08: columna repetida en el SET de un UPDATE. */
        public static final int ASIGNADA_VARIAS = 3;
        /** M08: columna repetida en la lista de un INSERT. */
        public static final int REPETIDA_EN_LISTA = 4;

        private Variante() {
        }
    }

    private static final String PLANTILLA_SUGERENCIA = " ¿Quisiste decir «%s»?";

    private final Severidad severidad;
    private final String[] plantillas;

    CodigoDiagnostico(Severidad severidad, String... plantillas) {
        this.severidad = severidad;
        this.plantillas = plantillas;
    }

    public Severidad severidad() {
        return severidad;
    }

    /** Crea un diagnóstico con la plantilla por defecto del código. */
    public Diagnostico crear(int linea, int columna, Object... argumentos) {
        return crearVariante(linea, columna, Variante.BASE, argumentos);
    }

    /** Crea un diagnóstico con la plantilla número {@code variante} (ver {@link Variante}). */
    public Diagnostico crearVariante(int linea, int columna, int variante, Object... argumentos) {
        String mensaje = String.format(Locale.ROOT, plantillas[variante], argumentos);
        return new Diagnostico(this, linea, columna, mensaje);
    }

    /** Texto que se agrega al final de un mensaje cuando hay un nombre parecido que sugerir. */
    static String sugerencia(String nombre) {
        return String.format(Locale.ROOT, PLANTILLA_SUGERENCIA, nombre);
    }
}
