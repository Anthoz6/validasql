package co.edu.curn.validasql.arbol;

/**
 * Raíz del árbol sintáctico: las cinco sentencias del lenguaje. Al ser una interfaz sellada, el
 * {@code switch} del analizador semántico tiene que cubrir los cinco casos o no compila.
 */
public sealed interface Sentencia
        permits CrearTabla, Seleccion, Insercion, Actualizacion, Eliminacion {
}
