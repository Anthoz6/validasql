package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

/**
 * {@code operando IS NULL} o {@code operando IS NOT NULL}.
 *
 * @param negado true si se escribió IS NOT NULL
 */
public record EsNulo(Operando operando, Token is, boolean negado) implements Condicion {
}
