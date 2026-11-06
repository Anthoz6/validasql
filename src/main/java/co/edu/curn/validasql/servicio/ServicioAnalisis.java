package co.edu.curn.validasql.servicio;

import co.edu.curn.validasql.arbol.Sentencia;
import co.edu.curn.validasql.lexico.AnalizadorLexico;
import co.edu.curn.validasql.lexico.ResultadoLexico;
import co.edu.curn.validasql.lexico.Segmentador;
import co.edu.curn.validasql.lexico.UnidadSentencia;
import co.edu.curn.validasql.reporte.Diagnostico;
import co.edu.curn.validasql.semantico.AnalizadorSemantico;
import co.edu.curn.validasql.semantico.TablaSimbolos;
import co.edu.curn.validasql.sintactico.AnalizadorSintactico;
import co.edu.curn.validasql.sintactico.PasoTraza;
import co.edu.curn.validasql.sintactico.ResultadoSintactico;

import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta las cuatro fases. Es el único punto de entrada al núcleo: la API REST, la consola y las
 * pruebas lo llaman igual, y no depende de Spring.
 *
 * <p>Primero se analiza el esquema y después las consultas, porque las consultas necesitan que la
 * tabla de símbolos ya tenga las tablas. Cada texto cuenta sus líneas desde 1. Una sentencia pasa a
 * la fase siguiente solo si la anterior no encontró errores, pero un error nunca impide analizar las
 * sentencias que vienen después.</p>
 *
 * <p>La clase no guarda estado: cada análisis crea una {@link TablaSimbolos} nueva y los analizadores
 * que guardan el estado de una lectura, así que se puede usar desde varios hilos.</p>
 */
public final class ServicioAnalisis {

    private final AnalizadorSemantico semantico = new AnalizadorSemantico();

    public ResultadoAnalisis analizar(String esquema, String consultas) {
        TablaSimbolos simbolos = new TablaSimbolos();
        List<ResultadoSentencia> resultadoEsquema = analizarTexto(esquema, simbolos);
        List<ResultadoSentencia> resultadoConsultas = analizarTexto(consultas, simbolos);
        return new ResultadoAnalisis(simbolos.nombres(), resultadoEsquema, resultadoConsultas);
    }

    /** Prog → Sent Prog | ε: una vuelta por cada sentencia que entrega el segmentador. */
    private List<ResultadoSentencia> analizarTexto(String texto, TablaSimbolos simbolos) {
        ResultadoLexico lexico = new AnalizadorLexico().analizar(texto);
        List<UnidadSentencia> unidades = new Segmentador().separar(lexico);

        List<ResultadoSentencia> resultados = new ArrayList<>();
        for (UnidadSentencia unidad : unidades) {
            resultados.add(analizarSentencia(unidad, simbolos));
        }
        return resultados;
    }

    private ResultadoSentencia analizarSentencia(UnidadSentencia unidad, TablaSimbolos simbolos) {
        if (unidad.tieneErroresLexicos()) {
            // Se reportan todos los errores léxicos y la sentencia no pasa al parser.
            return resultado(unidad, null, List.of(), unidad.erroresLexicos());
        }

        ResultadoSintactico sintactico = new AnalizadorSintactico().analizar(unidad);
        if (!sintactico.esValido()) {
            return resultado(unidad, null, sintactico.traza(), List.of(sintactico.error()));
        }

        List<Diagnostico> diagnosticos = semantico.analizar(sintactico.arbol(), simbolos);
        return resultado(unidad, sintactico.arbol(), sintactico.traza(), diagnosticos);
    }

    private static ResultadoSentencia resultado(UnidadSentencia unidad, Sentencia arbol, List<PasoTraza> traza,
                                                List<Diagnostico> diagnosticos) {
        return new ResultadoSentencia(unidad.numero(), unidad.linea(), unidad.columna(), unidad.texto(),
                unidad.tokens(), arbol, traza, diagnosticos);
    }
}
