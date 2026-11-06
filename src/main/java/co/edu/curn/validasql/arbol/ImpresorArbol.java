package co.edu.curn.validasql.arbol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dibuja el árbol sintáctico de una sentencia con el mismo estilo del ejemplo 1 del anexo:
 *
 * <pre>
 * Select
 * ├─ columnas: Col(nombre)
 * ├─ FROM: estudiante
 * └─ WHERE
 *    └─ Comparación (&gt;=)
 *       ├─ Col(semestre)
 *       └─ NUM_ENT(4)
 * </pre>
 */
public final class ImpresorArbol {

    /** Nodo del dibujo: una etiqueta y sus hijos. */
    private record Nodo(String etiqueta, List<Nodo> hijos) {
        Nodo(String etiqueta) {
            this(etiqueta, List.of());
        }
    }

    private ImpresorArbol() {
    }

    public static String imprimir(Sentencia sentencia) {
        Nodo raiz = switch (sentencia) {
            case CrearTabla c -> nodoCrear(c);
            case Seleccion s -> nodoSeleccion(s);
            case Insercion i -> nodoInsercion(i);
            case Actualizacion a -> nodoActualizacion(a);
            case Eliminacion e -> nodoEliminacion(e);
        };
        StringBuilder salida = new StringBuilder(raiz.etiqueta());
        dibujarHijos(raiz, "", salida);
        return salida.toString();
    }

    // ------------------------------------------------------------------ dibujo

    private static void dibujarHijos(Nodo padre, String prefijo, StringBuilder salida) {
        for (int i = 0; i < padre.hijos().size(); i++) {
            boolean ultimo = i == padre.hijos().size() - 1;
            Nodo hijo = padre.hijos().get(i);
            salida.append('\n').append(prefijo).append(ultimo ? "└─ " : "├─ ").append(hijo.etiqueta());
            dibujarHijos(hijo, prefijo + (ultimo ? "   " : "│  "), salida);
        }
    }

    // ------------------------------------------------------------------ sentencias

    private static Nodo nodoCrear(CrearTabla c) {
        List<Nodo> columnas = new ArrayList<>();
        for (DefColumna d : c.columnas()) {
            StringBuilder texto = new StringBuilder(d.nombre().lexema()).append(": ").append(d.tipo());
            if (d.tipo() == TipoDato.VARCHAR) {
                texto.append('(').append(d.longitud()).append(')');
            }
            if (d.clavePrimaria()) {
                texto.append(", PRIMARY KEY");
            }
            if (d.notNull()) {
                texto.append(", NOT NULL");
            }
            columnas.add(new Nodo("Columna(" + texto + ")"));
        }
        return new Nodo("CrearTabla(" + c.nombre().lexema() + ")", columnas);
    }

    private static Nodo nodoSeleccion(Seleccion s) {
        List<Nodo> hijos = new ArrayList<>();
        hijos.add(new Nodo("columnas: " + (s.asterisco() != null
                ? "*"
                : s.columnas().stream().map(ImpresorArbol::etiquetaColumna).collect(Collectors.joining(", ")))));
        hijos.add(new Nodo("FROM: " + s.tabla().lexema()));
        if (s.join() != null) {
            hijos.add(new Nodo("INNER JOIN: " + s.join().tabla().lexema(),
                    List.of(new Nodo("ON", List.of(nodoCondicion(s.join().condicion()))))));
        }
        if (s.where() != null) {
            hijos.add(new Nodo("WHERE", List.of(nodoCondicion(s.where()))));
        }
        if (s.orden() != null) {
            String direccion = s.orden().direccion() != null ? " " + s.orden().direccion().lexema().toUpperCase() : "";
            hijos.add(new Nodo("ORDER BY: " + etiquetaColumna(s.orden().columna()) + direccion));
        }
        return new Nodo("Select", hijos);
    }

    private static Nodo nodoInsercion(Insercion i) {
        List<Nodo> hijos = new ArrayList<>();
        hijos.add(new Nodo("INTO: " + i.tabla().lexema()));
        if (i.columnas() != null) {
            hijos.add(new Nodo("columnas: " + i.columnas().stream()
                    .map(t -> t.lexema()).collect(Collectors.joining(", "))));
        }
        hijos.add(new Nodo("VALUES: " + i.valores().stream()
                .map(ImpresorArbol::etiquetaLiteral).collect(Collectors.joining(", "))));
        return new Nodo("Insert", hijos);
    }

    private static Nodo nodoActualizacion(Actualizacion a) {
        List<Nodo> hijos = new ArrayList<>();
        hijos.add(new Nodo("tabla: " + a.tabla().lexema()));
        hijos.add(new Nodo("SET: " + a.asignaciones().stream()
                .map(x -> x.columna().lexema() + " = " + etiquetaLiteral(x.valor()))
                .collect(Collectors.joining(", "))));
        if (a.where() != null) {
            hijos.add(new Nodo("WHERE", List.of(nodoCondicion(a.where()))));
        }
        return new Nodo("Update", hijos);
    }

    private static Nodo nodoEliminacion(Eliminacion e) {
        List<Nodo> hijos = new ArrayList<>();
        hijos.add(new Nodo("FROM: " + e.tabla().lexema()));
        if (e.where() != null) {
            hijos.add(new Nodo("WHERE", List.of(nodoCondicion(e.where()))));
        }
        return new Nodo("Delete", hijos);
    }

    // ------------------------------------------------------------------ condiciones y operandos

    private static Nodo nodoCondicion(Condicion condicion) {
        return switch (condicion) {
            case Logica l -> new Nodo("Lógica (" + l.operador().lexema().toUpperCase() + ")",
                    List.of(nodoCondicion(l.izquierda()), nodoCondicion(l.derecha())));
            case Negacion n -> new Nodo("Negación (NOT)", List.of(nodoCondicion(n.condicion())));
            case Comparacion c -> new Nodo("Comparación (" + c.operador().lexema() + ")",
                    List.of(nodoOperando(c.izquierdo()), nodoOperando(c.derecho())));
            case EsNulo e -> new Nodo(e.negado() ? "IS NOT NULL" : "IS NULL", List.of(nodoOperando(e.operando())));
        };
    }

    private static Nodo nodoOperando(Operando operando) {
        return switch (operando) {
            case ReferenciaColumna r -> new Nodo(etiquetaColumna(r));
            case Literal l -> new Nodo(etiquetaLiteral(l));
        };
    }

    private static String etiquetaColumna(ReferenciaColumna columna) {
        return "Col(" + columna.textoOriginal() + ")";
    }

    /** Literales con su tipo de token, como en el anexo: {@code NUM_ENT(4)}. TRUE, FALSE y NULL van solos. */
    private static String etiquetaLiteral(Literal literal) {
        return switch (literal.token().tipo()) {
            case NUM_ENT, NUM_DEC, CADENA -> literal.token().tipo() + "(" + literal.token().lexema() + ")";
            default -> literal.token().lexema().toUpperCase();
        };
    }
}
