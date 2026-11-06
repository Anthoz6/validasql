package co.edu.curn.validasql.reporte;

/**
 * Da a cada diagnóstico el formato del anexo técnico:
 * {@code [CÓDIGO] Tipo (línea L, columna C). Mensaje}
 */
public final class FormateadorReporte {

    private FormateadorReporte() {
    }

    public static String formatear(Diagnostico d) {
        return "[" + d.codigo().name() + "] " + d.severidad().etiqueta()
                + " (línea " + d.linea() + ", columna " + d.columna() + "). " + d.mensaje();
    }
}
