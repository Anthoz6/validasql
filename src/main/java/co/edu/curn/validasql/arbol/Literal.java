package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;
import co.edu.curn.validasql.lexico.TipoToken;

/** Valor escrito directamente en la sentencia: número, cadena, TRUE, FALSE o NULL. */
public record Literal(Token token) implements Operando {

    /** Clase de valor, sin distinguir entre entero y decimal. */
    public enum Clase { NUMERO, TEXTO, LOGICO, NULO }

    @Override
    public Token primerToken() {
        return token;
    }

    public Clase clase() {
        return switch (token.tipo()) {
            case NUM_ENT, NUM_DEC -> Clase.NUMERO;
            case CADENA -> Clase.TEXTO;
            case TRUE, FALSE -> Clase.LOGICO;
            case NULL -> Clase.NULO;
            default -> throw new IllegalStateException("El token " + token.tipo() + " no es un literal");
        };
    }

    public boolean esNulo() {
        return token.tipo() == TipoToken.NULL;
    }

    /** Contenido de una cadena sin las comillas. Para los demás literales, el lexema. */
    public String contenido() {
        String lexema = token.lexema();
        return token.tipo() == TipoToken.CADENA ? lexema.substring(1, lexema.length() - 1) : lexema;
    }
}
