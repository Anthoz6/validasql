package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

/** Lado de una comparación: una columna o un literal. */
public sealed interface Operando permits ReferenciaColumna, Literal {

    /** Primer token del operando, que es donde se informan los errores sobre él. */
    Token primerToken();
}
