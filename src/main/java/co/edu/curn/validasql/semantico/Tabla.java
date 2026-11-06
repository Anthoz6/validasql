package co.edu.curn.validasql.semantico;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Una tabla de la tabla de símbolos. Las columnas se guardan en un {@link LinkedHashMap} para
 * conservar el orden en que se declararon, que hace falta en un INSERT sin lista de columnas.
 */
public final class Tabla {

    private final String nombre;
    private final Map<String, Columna> columnas;

    /** @param columnas columnas en orden de declaración, con el nombre ya en minúscula como clave */
    public Tabla(String nombre, Map<String, Columna> columnas) {
        this.nombre = nombre.toLowerCase(Locale.ROOT);
        this.columnas = Collections.unmodifiableMap(new LinkedHashMap<>(columnas));
    }

    public String nombre() {
        return nombre;
    }

    /** Columnas en el orden en que se declararon. */
    public List<Columna> columnas() {
        return new ArrayList<>(columnas.values());
    }

    public Optional<Columna> columna(String nombreColumna) {
        return Optional.ofNullable(columnas.get(nombreColumna.toLowerCase(Locale.ROOT)));
    }

    public List<String> nombresDeColumnas() {
        return new ArrayList<>(columnas.keySet());
    }
}
