package co.edu.curn.validasql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Comprueba la tabla de dependencias entre paquetes del documento de diseño (sección 2.2) leyendo
 * los {@code import} del código fuente: cada paquete solo puede usar los que su fila permite, y
 * solo {@code web} importa Spring.
 */
class ArquitecturaTest {

    private static final String BASE = "co.edu.curn.validasql";
    private static final Path FUENTES = Path.of("src/main/java", BASE.replace('.', '/'));
    private static final Pattern IMPORT = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+);", Pattern.MULTILINE);

    private static final Map<String, Set<String>> PERMITIDOS = Map.of(
            "reporte", Set.of(),
            "lexico", Set.of("reporte"),
            "arbol", Set.of("lexico"),
            "sintactico", Set.of("lexico", "arbol", "reporte"),
            "semantico", Set.of("lexico", "arbol", "reporte"),
            "servicio", Set.of("reporte", "lexico", "arbol", "sintactico", "semantico"),
            "web", Set.of("servicio"),
            "consola", Set.of("servicio"));

    @Test
    @DisplayName("Cada paquete importa solo los paquetes que le permite el diseño")
    void dependenciasEnUnSoloSentido() throws IOException {
        List<String> violaciones = new ArrayList<>();
        for (String paquete : PERMITIDOS.keySet()) {
            for (Path archivo : archivosDe(paquete)) {
                for (String importado : importsDe(archivo)) {
                    if (!importado.startsWith(BASE + ".")) {
                        continue;
                    }
                    String destino = importado.substring(BASE.length() + 1).split("\\.")[0];
                    if (!destino.equals(paquete) && !PERMITIDOS.get(paquete).contains(destino)) {
                        violaciones.add(archivo.getFileName() + " (" + paquete + ") importa " + importado);
                    }
                }
            }
        }
        assertEquals(List.of(), violaciones);
    }

    @Test
    @DisplayName("Solo el paquete web importa Spring")
    void soloWebUsaSpring() throws IOException {
        List<String> violaciones = new ArrayList<>();
        for (String paquete : PERMITIDOS.keySet()) {
            if (paquete.equals("web")) {
                continue;
            }
            for (Path archivo : archivosDe(paquete)) {
                importsDe(archivo).stream()
                        .filter(i -> i.startsWith("org.springframework"))
                        .forEach(i -> violaciones.add(archivo.getFileName() + " importa " + i));
            }
        }
        assertEquals(List.of(), violaciones);
    }

    @Test
    @DisplayName("No hay paquetes en el código fuente fuera de los ocho del diseño")
    void sinPaquetesExtra() throws IOException {
        try (Stream<Path> hijos = Files.list(FUENTES)) {
            List<String> encontrados = hijos.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString()).sorted().toList();
            assertEquals(PERMITIDOS.keySet().stream().sorted().toList(), encontrados);
        }
    }

    @Test
    @DisplayName("El núcleo no usa librerías de análisis (ANTLR, JSqlParser ni similares)")
    void sinLibreriasDeAnalisis() throws IOException {
        for (String paquete : PERMITIDOS.keySet()) {
            for (Path archivo : archivosDe(paquete)) {
                for (String importado : importsDe(archivo)) {
                    assertFalse(importado.startsWith("org.antlr") || importado.startsWith("net.sf.jsqlparser")
                            || importado.startsWith("com.github.jsqlparser"), archivo + " importa " + importado);
                }
            }
        }
    }

    private static List<Path> archivosDe(String paquete) throws IOException {
        try (Stream<Path> archivos = Files.list(FUENTES.resolve(paquete))) {
            return archivos.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    private static List<String> importsDe(Path archivo) throws IOException {
        Matcher m = IMPORT.matcher(Files.readString(archivo));
        List<String> imports = new ArrayList<>();
        while (m.find()) {
            imports.add(m.group(1));
        }
        return imports;
    }
}
