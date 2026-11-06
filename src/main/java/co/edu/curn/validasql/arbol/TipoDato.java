package co.edu.curn.validasql.arbol;

/** Los cinco tipos de dato que admite CREATE TABLE. */
public enum TipoDato {
    INT, DECIMAL, VARCHAR, DATE, BOOLEAN;

    /** INT y DECIMAL son compatibles entre sí. */
    public boolean esNumerico() {
        return this == INT || this == DECIMAL;
    }
}
