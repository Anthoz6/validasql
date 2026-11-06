package co.edu.curn.validasql.lexico;

import co.edu.curn.validasql.reporte.CodigoDiagnostico;
import co.edu.curn.validasql.reporte.Diagnostico;

import java.util.ArrayList;
import java.util.List;

/**
 * Separa los tokens en sentencias para que una sentencia con errores no impida analizar las demás.
 *
 * <p>Una sentencia termina en {@code ;} o al final del texto. La excepción es la cadena sin cerrar
 * (L02): como el analizador léxico la lee hasta el final de la línea, se pudo haber tragado el
 * {@code ;}. Por eso, tras un L02 la sentencia se cierra al final de esa línea y la línea siguiente
 * empieza una sentencia nueva.</p>
 */
public final class Segmentador {

    public List<UnidadSentencia> separar(ResultadoLexico lexico) {
        IndiceLineas lineas = new IndiceLineas(lexico.fuente());
        List<UnidadSentencia> unidades = new ArrayList<>();
        Acumulador actual = null;

        int t = 0;
        int e = 0;
        List<Token> tokens = lexico.tokens();
        List<Diagnostico> errores = lexico.errores();

        // Se recorren tokens y errores en el orden en que aparecen en el texto.
        while (t < tokens.size() || e < errores.size()) {
            boolean sigueToken = e >= errores.size()
                    || (t < tokens.size() && antes(tokens.get(t), errores.get(e)));
            int lineaEvento = sigueToken ? tokens.get(t).linea() : errores.get(e).linea();

            // Si la sentencia abierta terminó en un L02, todo lo que viene en una línea posterior
            // pertenece a una sentencia nueva.
            if (actual != null && actual.lineaDeCorte > 0 && lineaEvento > actual.lineaDeCorte) {
                unidades.add(actual.cerrar(unidades.size() + 1, lexico, lineas, lineas.finDeLinea(actual.lineaDeCorte)));
                actual = null;
            }
            if (actual == null) {
                actual = new Acumulador();
            }

            if (sigueToken) {
                Token token = tokens.get(t++);
                actual.agregar(token.linea(), token.columna(), lineas);
                actual.tokens.add(token);
                if (token.tipo() == TipoToken.PUNTOCOMA) {
                    unidades.add(actual.cerrar(unidades.size() + 1, lexico, lineas,
                            lineas.desplazamiento(token.linea(), token.columnaFinal())));
                    actual = null;
                }
            } else {
                Diagnostico error = errores.get(e++);
                actual.agregar(error.linea(), error.columna(), lineas);
                actual.errores.add(error);
                if (error.codigo() == CodigoDiagnostico.L02) {
                    actual.lineaDeCorte = error.linea();
                }
            }
        }

        if (actual != null) {
            int fin = actual.lineaDeCorte > 0 ? lineas.finDeLinea(actual.lineaDeCorte) : lexico.fuente().length();
            unidades.add(actual.cerrar(unidades.size() + 1, lexico, lineas, fin));
        }
        return unidades;
    }

    private static boolean antes(Token token, Diagnostico error) {
        return token.linea() < error.linea()
                || (token.linea() == error.linea() && token.columna() < error.columna());
    }

    /** Sentencia que se está armando. */
    private static final class Acumulador {
        private final List<Token> tokens = new ArrayList<>();
        private final List<Diagnostico> errores = new ArrayList<>();
        private int linea;
        private int columna;
        private int desplazamientoInicial = -1;
        /** Línea del L02 que obliga a cerrar la sentencia; 0 si no hubo. */
        private int lineaDeCorte;

        void agregar(int lineaEvento, int columnaEvento, IndiceLineas lineas) {
            if (desplazamientoInicial < 0) {
                linea = lineaEvento;
                columna = columnaEvento;
                desplazamientoInicial = lineas.desplazamiento(lineaEvento, columnaEvento);
            }
        }

        UnidadSentencia cerrar(int numero, ResultadoLexico lexico, IndiceLineas lineas, int desplazamientoFinal) {
            String texto = lexico.fuente().substring(desplazamientoInicial, desplazamientoFinal).stripTrailing();
            return new UnidadSentencia(numero, linea, columna, texto, List.copyOf(tokens), List.copyOf(errores));
        }
    }
}
