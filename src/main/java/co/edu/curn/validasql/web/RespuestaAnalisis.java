package co.edu.curn.validasql.web;

import co.edu.curn.validasql.servicio.ResultadoAnalisis;
import co.edu.curn.validasql.servicio.ResultadoSentencia;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de {@code POST /api/analizar}: el resultado del servicio convertido en datos simples
 * para JSON. Este paquete solo conoce el paquete {@code servicio}, así que los tokens, los
 * diagnósticos y los pasos de la traza se convierten dentro de las expresiones lambda sin
 * nombrar sus tipos.
 */
public record RespuestaAnalisis(Resumen resumen, List<SentenciaDto> esquema, List<SentenciaDto> consultas) {

    public record Resumen(int tablas, int sentencias, int validas, int conAdvertencias, int conErrores) {
    }

    public record SentenciaDto(int numero, int linea, int columna, String estado, String texto,
                               List<TokenDto> tokens, String arbol, List<PasoDto> traza,
                               List<DiagnosticoDto> diagnosticos) {
    }

    public record TokenDto(String tipo, String lexema, int linea, int columna) {
    }

    public record PasoDto(int paso, String noTerminal, String token, String produccion, String motivo) {
    }

    public record DiagnosticoDto(String codigo, String severidad, int linea, int columna, String texto) {
    }

    public static RespuestaAnalisis de(ResultadoAnalisis resultado) {
        var r = resultado.resumen();
        return new RespuestaAnalisis(
                new Resumen(r.tablas(), r.sentencias(), r.validas(), r.conAdvertencias(), r.conErrores()),
                resultado.esquema().stream().map(RespuestaAnalisis::sentencia).toList(),
                resultado.consultas().stream().map(RespuestaAnalisis::sentencia).toList());
    }

    private static SentenciaDto sentencia(ResultadoSentencia s) {
        return new SentenciaDto(s.numero(), s.linea(), s.columna(), s.estado().name(), s.texto(),
                s.tokens().stream().map(t -> new TokenDto(t.tipo().name(), t.lexema(), t.linea(), t.columna())).toList(),
                s.arbolTexto(),
                s.traza().stream().map(p -> new PasoDto(p.paso(), p.noTerminal(), p.token(), p.produccion(),
                        p.motivo().name())).toList(),
                diagnosticos(s));
    }

    /** Cada diagnóstico lleva su código, su posición y el texto completo con el formato del anexo. */
    private static List<DiagnosticoDto> diagnosticos(ResultadoSentencia s) {
        List<String> textos = s.reportes();
        List<DiagnosticoDto> lista = new ArrayList<>();
        for (int i = 0; i < textos.size(); i++) {
            var d = s.diagnosticos().get(i);
            lista.add(new DiagnosticoDto(d.codigo().name(), d.severidad().name(), d.linea(), d.columna(), textos.get(i)));
        }
        return lista;
    }
}
