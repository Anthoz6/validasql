package co.edu.curn.validasql.consola;

import co.edu.curn.validasql.servicio.ResultadoAnalisis;
import co.edu.curn.validasql.servicio.ResultadoSentencia;
import co.edu.curn.validasql.servicio.ServicioAnalisis;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente de consola. Lee dos archivos, el esquema y las consultas, los analiza con el mismo
 * {@link ServicioAnalisis} que usa la API web e imprime el resultado de cada sentencia.
 *
 * <pre>
 * Uso: Consola [--tokens] [--arbol] [--traza] esquema.sql consultas.sql
 * </pre>
 *
 * <p>Las opciones muestran lo que produjo cada fase: los tokens del análisis léxico, el árbol del
 * análisis sintáctico y la traza de decisiones del parser LL(1).</p>
 */
public final class Consola {

    private static final String USO = "Uso: Consola [--tokens] [--arbol] [--traza] esquema.sql consultas.sql";

    private Consola() {
    }

    public static void main(String[] args) throws IOException {
        // La salida va siempre en UTF-8 para que se vean bien las tildes y los símbolos del árbol.
        PrintStream salida = new PrintStream(System.out, true, StandardCharsets.UTF_8);

        boolean tokens = false;
        boolean arbol = false;
        boolean traza = false;
        List<String> archivos = new ArrayList<>();
        for (String argumento : args) {
            switch (argumento) {
                case "--tokens" -> tokens = true;
                case "--arbol" -> arbol = true;
                case "--traza" -> traza = true;
                default -> archivos.add(argumento);
            }
        }
        if (archivos.size() != 2) {
            salida.println(USO);
            System.exit(2);
        }

        String esquema = Files.readString(Path.of(archivos.get(0)), StandardCharsets.UTF_8);
        String consultas = Files.readString(Path.of(archivos.get(1)), StandardCharsets.UTF_8);
        ejecutar(esquema, consultas, tokens, arbol, traza, salida);
    }

    /** Analiza e imprime. Separado de {@code main} para poder probarlo sin archivos. */
    static void ejecutar(String esquema, String consultas, boolean tokens, boolean arbol, boolean traza,
                         PrintStream salida) {
        ResultadoAnalisis resultado = new ServicioAnalisis().analizar(esquema, consultas);

        imprimirEsquema(resultado, salida);
        for (ResultadoSentencia sentencia : resultado.consultas()) {
            imprimirSentencia(sentencia, tokens, arbol, traza, salida);
        }
        var resumen = resultado.resumen();
        salida.println();
        salida.printf("Resumen: sentencias %d, válidas %d, con advertencias %d, con errores %d.%n",
                resumen.sentencias(), resumen.validas(), resumen.conAdvertencias(), resumen.conErrores());
    }

    private static void imprimirEsquema(ResultadoAnalisis resultado, PrintStream salida) {
        List<String> problemas = new ArrayList<>();
        resultado.esquema().forEach(s -> problemas.addAll(s.reportes()));
        salida.printf("Esquema: %d %s cargadas (%s). %s%n", resultado.tablas(),
                resultado.tablas() == 1 ? "tabla" : "tablas", String.join(", ", resultado.nombresTablas()),
                problemas.isEmpty() ? "Sin errores." : "Con problemas:");
        problemas.forEach(p -> salida.println("  " + p));
    }

    private static void imprimirSentencia(ResultadoSentencia s, boolean tokens, boolean arbol, boolean traza,
                                          PrintStream salida) {
        salida.println();
        salida.printf("Sentencia %d (línea %d): %s%n", s.numero(), s.linea(), s.texto().replaceAll("\\s+", " "));

        if (tokens) {
            salida.println("  Tokens:");
            s.tokens().forEach(t -> salida.printf("    %-10s %-14s línea %d, columna %d%n",
                    t.tipo(), t.lexema(), t.linea(), t.columna()));
        }
        if (arbol && s.arbol() != null) {
            salida.println("  Árbol:");
            s.arbolTexto().lines().forEach(linea -> salida.println("    " + linea));
        }
        if (traza && !s.traza().isEmpty()) {
            salida.println("  Traza LL(1):");
            s.traza().forEach(p -> salida.printf("    %2d  %-8s %-12s %-48s %s%n",
                    p.paso(), p.noTerminal(), p.token(), p.produccion(), p.motivo()));
        }

        if (s.reportes().isEmpty()) {
            salida.println("  Sentencia válida. Sin errores ni advertencias.");
        } else {
            s.reportes().forEach(r -> salida.println("  " + r));
        }
    }
}
