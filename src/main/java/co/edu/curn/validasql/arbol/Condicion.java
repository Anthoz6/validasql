package co.edu.curn.validasql.arbol;

/**
 * Condición de un WHERE o de un ON. Los paréntesis no tienen nodo propio: solo cambian la forma
 * del árbol.
 */
public sealed interface Condicion permits Logica, Negacion, Comparacion, EsNulo {
}
