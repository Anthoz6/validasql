package co.edu.curn.validasql;

import co.edu.curn.validasql.lexico.TipoToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La página colorea las palabras reservadas con una lista propia; esta prueba evita que se desfase del lexer. */
class PaginaWebTest {

    @Test
    @DisplayName("Las palabras reservadas del resaltado de app.js son las 32 de TipoToken")
    void palabrasDelResaltadoCoincidenConElLexer() throws IOException {
        String js = Files.readString(Path.of("src/main/resources/static/app.js"));
        Matcher bloque = Pattern.compile("PALABRAS_RESERVADAS = new Set\\(\\[(.*?)\\]\\)", Pattern.DOTALL).matcher(js);
        assertTrue(bloque.find(), "no se encontró PALABRAS_RESERVADAS en app.js");

        Set<String> enLaPagina = new TreeSet<>();
        Matcher palabra = Pattern.compile("\"([A-Z]+)\"").matcher(bloque.group(1));
        while (palabra.find()) {
            enLaPagina.add(palabra.group(1));
        }

        Set<String> enElLexer = new TreeSet<>();
        Arrays.stream(TipoToken.values()).filter(TipoToken::esPalabraReservada).forEach(t -> enElLexer.add(t.name()));

        assertEquals(32, enElLexer.size());
        assertEquals(enElLexer, enLaPagina);
    }
}
