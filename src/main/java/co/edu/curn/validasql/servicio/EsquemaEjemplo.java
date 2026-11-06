package co.edu.curn.validasql.servicio;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * El esquema de la universidad y unas consultas de prueba, que carga el botón «Cargar ejemplo».
 * Los textos viven en {@code src/main/resources/ejemplos}.
 */
public final class EsquemaEjemplo {

    private static final String ESQUEMA = "/ejemplos/universidad.sql";
    private static final String CONSULTAS = "/ejemplos/consultas.sql";

    private EsquemaEjemplo() {
    }

    /** Las cinco sentencias CREATE TABLE de la universidad. */
    public static String esquema() {
        return leer(ESQUEMA);
    }

    /** Consultas de ejemplo: válidas, con errores y con advertencias. */
    public static String consultas() {
        return leer(CONSULTAS);
    }

    private static String leer(String recurso) {
        try (InputStream entrada = EsquemaEjemplo.class.getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontró el recurso " + recurso);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + recurso, e);
        }
    }
}
