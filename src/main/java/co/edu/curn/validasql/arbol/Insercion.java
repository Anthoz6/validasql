package co.edu.curn.validasql.arbol;

import co.edu.curn.validasql.lexico.Token;

import java.util.List;

/**
 * {@code INSERT INTO tabla [(columnas)] VALUES (valores)}.
 *
 * @param tabla    token con el nombre de la tabla
 * @param columnas tokens de la lista de columnas; {@code null} si no se escribió lista
 * @param values   token VALUES, donde se informan M06 y M07
 * @param valores  literales de VALUES
 */
public record Insercion(Token tabla, List<Token> columnas, Token values, List<Literal> valores)
        implements Sentencia {
}
