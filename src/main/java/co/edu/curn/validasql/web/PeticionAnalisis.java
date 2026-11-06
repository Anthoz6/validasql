package co.edu.curn.validasql.web;

/**
 * Cuerpo de {@code POST /api/analizar}. Si un campo falta se trata como texto vacío.
 *
 * @param esquema   sentencias CREATE TABLE
 * @param consultas sentencias a validar
 */
public record PeticionAnalisis(String esquema, String consultas) {
}
