package co.edu.curn.validasql.servicio;

import co.edu.curn.validasql.arbol.ImpresorArbol;
import co.edu.curn.validasql.arbol.Sentencia;
import co.edu.curn.validasql.lexico.Token;
import co.edu.curn.validasql.reporte.Diagnostico;
import co.edu.curn.validasql.reporte.FormateadorReporte;
import co.edu.curn.validasql.sintactico.PasoTraza;

import java.util.List;

/**
 * Lo que produjo cada fase para una sentencia.
 *
 * @param numero       número de la sentencia dentro de su texto, desde 1
 * @param linea        línea donde empieza
 * @param columna      columna donde empieza
 * @param texto        texto de la sentencia
 * @param tokens       tokens de la sentencia
 * @param arbol        árbol sintáctico; {@code null} si hubo errores léxicos o sintácticos
 * @param traza        decisiones del parser LL(1); vacía si hubo errores léxicos
 * @param diagnosticos errores y advertencias de todas las fases, ordenados por posición
 */
public record ResultadoSentencia(int numero, int linea, int columna, String texto, List<Token> tokens,
                                 Sentencia arbol, List<PasoTraza> traza, List<Diagnostico> diagnosticos) {

    /** Resultado de una sentencia: sin problemas, solo advertencias o con algún error. */
    public enum Estado { VALIDA, CON_ADVERTENCIAS, CON_ERRORES }

    public Estado estado() {
        if (diagnosticos.stream().anyMatch(d -> d.severidad().esError())) {
            return Estado.CON_ERRORES;
        }
        return diagnosticos.isEmpty() ? Estado.VALIDA : Estado.CON_ADVERTENCIAS;
    }

    /** Los diagnósticos con el formato del anexo: {@code [M02] Error semántico (línea 1, columna 8). ...}. */
    public List<String> reportes() {
        return diagnosticos.stream().map(FormateadorReporte::formatear).toList();
    }

    /** Árbol sintáctico dibujado como texto; cadena vacía si la sentencia no llegó a tener árbol. */
    public String arbolTexto() {
        return arbol == null ? "" : ImpresorArbol.imprimir(arbol);
    }
}
