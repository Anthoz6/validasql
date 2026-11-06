package co.edu.curn.validasql.lexico;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabla con el desplazamiento donde empieza cada línea de un texto. Permite pasar de
 * (línea, columna) a una posición dentro del texto, para recortar el texto de cada sentencia.
 */
final class IndiceLineas {

    private final String texto;
    private final List<Integer> inicios = new ArrayList<>();

    IndiceLineas(String texto) {
        this.texto = texto;
        inicios.add(0);
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == '\n') {
                inicios.add(i + 1);
            }
        }
    }

    /** Posición en el texto del carácter (linea, columna); ambas cuentan desde 1. */
    int desplazamiento(int linea, int columna) {
        return inicios.get(linea - 1) + columna - 1;
    }

    /** Posición donde termina la línea, justo antes del salto de línea o al final del texto. */
    int finDeLinea(int linea) {
        int fin = linea < inicios.size() ? inicios.get(linea) - 1 : texto.length();
        return fin > 0 && texto.charAt(fin - 1) == '\r' ? fin - 1 : fin;
    }
}
