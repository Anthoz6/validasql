package co.edu.curn.validasql.semantico;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Tabla de símbolos: las tablas que se han declarado con CREATE TABLE, por nombre en minúscula
 * (el lenguaje no distingue mayúsculas). Se crea una nueva por cada análisis, así que nada queda
 * guardado entre una petición y la siguiente. Solo el analizador semántico escribe en ella.
 */
public final class TablaSimbolos {

    private final Map<String, Tabla> tablas = new LinkedHashMap<>();

    public void registrar(Tabla tabla) {
        tablas.put(tabla.nombre(), tabla);
    }

    public Optional<Tabla> buscar(String nombre) {
        return Optional.ofNullable(tablas.get(nombre.toLowerCase(Locale.ROOT)));
    }

    public boolean existe(String nombre) {
        return tablas.containsKey(nombre.toLowerCase(Locale.ROOT));
    }

    public int cantidad() {
        return tablas.size();
    }

    /** Nombres de las tablas en el orden en que se declararon. */
    public List<String> nombres() {
        return new ArrayList<>(tablas.keySet());
    }
}
