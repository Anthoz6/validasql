package co.edu.curn.validasql;

import co.edu.curn.validasql.semantico.Sugeridor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SugeridorTest {

    private final Sugeridor sugeridor = new Sugeridor();

    @Test
    @DisplayName("Distancia de Levenshtein: casos conocidos")
    void distancia() {
        assertEquals(0, Sugeridor.distancia("tabla", "tabla"));
        assertEquals(1, Sugeridor.distancia("nombr", "nombre"));
        assertEquals(1, Sugeridor.distancia("estudiantes", "estudiante"));
        assertEquals(3, Sugeridor.distancia("kitten", "sitting"));
        assertEquals(3, Sugeridor.distancia("", "abc"));
        assertEquals(3, Sugeridor.distancia("abc", ""));
    }

    @Test
    @DisplayName("Sugiere el candidato más cercano si está a 2 ediciones o menos")
    void sugiere() {
        List<String> columnas = List.of("id", "nombre", "semestre", "id_programa");
        assertEquals(Optional.of("nombre"), sugeridor.sugerir("nombr", columnas));
        assertEquals(Optional.of("semestre"), sugeridor.sugerir("semestr", columnas));
        assertEquals(Optional.of("nombre"), sugeridor.sugerir("NOMBRA", columnas), "sin distinguir mayúsculas");
        assertEquals(Optional.of("nombre"), sugeridor.sugerir("nmbre", columnas));
    }

    @Test
    @DisplayName("No sugiere cuando todos están a más de 2 ediciones")
    void noSugiereLejos() {
        assertEquals(Optional.empty(), sugeridor.sugerir("apellido", List.of("id", "nombre", "semestre")));
        assertEquals(Optional.empty(), sugeridor.sugerir("x", List.of()));
    }

    @Test
    @DisplayName("En un empate gana el primero de la colección")
    void empate() {
        assertEquals(Optional.of("cat"), sugeridor.sugerir("bat", List.of("cat", "hat")));
    }
}
