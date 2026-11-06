package co.edu.curn.validasql.semantico;

import co.edu.curn.validasql.arbol.Literal;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Reglas de compatibilidad de tipos del anexo (sección 7):
 * <ul>
 *   <li>INT y DECIMAL aceptan NUM_ENT o NUM_DEC.</li>
 *   <li>VARCHAR(n) acepta una CADENA de hasta n caracteres, sin contar las comillas.</li>
 *   <li>DATE acepta una CADENA con formato 'AAAA-MM-DD' y una fecha real.</li>
 *   <li>BOOLEAN acepta TRUE o FALSE.</li>
 *   <li>NULL se acepta con cualquier tipo; que la columna sea NOT NULL lo revisa el analizador semántico.</li>
 *   <li>Entre dos columnas, ambas numéricas o del mismo tipo.</li>
 * </ul>
 */
public final class VerificadorTipos {

    /** Por qué un valor no cabe en una columna. */
    public enum Resultado { COMPATIBLE, TIPO_INCOMPATIBLE, TEXTO_LARGO, FECHA_INVALIDA }

    private static final Pattern FORMATO_FECHA = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /**
     * @param validarLongitud true en INSERT y UPDATE; false en comparaciones, donde comparar contra
     *                        un texto largo no es un error de tipos
     */
    public Resultado evaluar(Columna columna, Literal valor, boolean validarLongitud) {
        if (valor.esNulo()) {
            return Resultado.COMPATIBLE;
        }
        Literal.Clase clase = valor.clase();
        return switch (columna.tipo()) {
            case INT, DECIMAL -> clase == Literal.Clase.NUMERO ? Resultado.COMPATIBLE : Resultado.TIPO_INCOMPATIBLE;
            case BOOLEAN -> clase == Literal.Clase.LOGICO ? Resultado.COMPATIBLE : Resultado.TIPO_INCOMPATIBLE;
            case VARCHAR -> {
                if (clase != Literal.Clase.TEXTO) {
                    yield Resultado.TIPO_INCOMPATIBLE;
                }
                yield validarLongitud && valor.contenido().length() > columna.longitud()
                        ? Resultado.TEXTO_LARGO
                        : Resultado.COMPATIBLE;
            }
            case DATE -> {
                if (clase != Literal.Clase.TEXTO) {
                    yield Resultado.TIPO_INCOMPATIBLE;
                }
                yield esFechaReal(valor.contenido()) ? Resultado.COMPATIBLE : Resultado.FECHA_INVALIDA;
            }
        };
    }

    /** Un valor se puede guardar en la columna (incluye el límite de longitud de VARCHAR). */
    public boolean compatible(Columna columna, Literal valor) {
        return evaluar(columna, valor, true) == Resultado.COMPATIBLE;
    }

    /** Dos columnas se pueden comparar si ambas son numéricas o si tienen el mismo tipo. */
    public boolean comparables(Columna a, Columna b) {
        return (a.tipo().esNumerico() && b.tipo().esNumerico()) || a.tipo() == b.tipo();
    }

    /** Dos literales se pueden comparar si son de la misma clase o si alguno es NULL. */
    public boolean comparables(Literal a, Literal b) {
        return a.esNulo() || b.esNulo() || a.clase() == b.clase();
    }

    private static boolean esFechaReal(String texto) {
        if (!FORMATO_FECHA.matcher(texto).matches()) {
            return false;
        }
        try {
            LocalDate.parse(texto);                     // rechaza fechas como 2026-02-30
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
