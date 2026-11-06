package co.edu.curn.validasql.sintactico;

import co.edu.curn.validasql.arbol.Sentencia;
import co.edu.curn.validasql.reporte.Diagnostico;

import java.util.List;

/**
 * Salida del análisis sintáctico de una sentencia.
 *
 * @param arbol  árbol sintáctico; {@code null} si hubo error
 * @param traza  decisiones del parser; si hubo error, llega hasta el punto del error
 * @param error  el primer error sintáctico (S01); {@code null} si la sentencia es válida
 */
public record ResultadoSintactico(Sentencia arbol, List<PasoTraza> traza, Diagnostico error) {

    public boolean esValido() {
        return error == null;
    }
}
