# Tabla de casos y resultados

Casos válidos, inválidos y límite que se ejecutaron contra ValidaSQL. Cada fila es una prueba automática
que compara la salida completa (código, línea, columna y mensaje), así que la columna «Resultado» dice
si lo obtenido coincide con lo esperado. Última ejecución: `./mvnw test`, **127 pruebas, 0 fallos**.

Salvo que se indique otro esquema, todos usan el esquema común del anexo (`programa` y `estudiante`).
Las posiciones se cuentan desde 1.

## 1. Ejemplos del anexo técnico (clase 2)

| # | Tipo | Entrada | Salida esperada | Resultado | Prueba |
|---|---|---|---|---|---|
| 1 | Válido | `SELECT nombre FROM estudiante WHERE semestre >= 4;` | Sin errores. 9 tokens, árbol del anexo, 22 decisiones LL(1) | Coincide | `ejemplo1ConsultaValida`, `ejemplo1Tokens` |
| 2 | Válido | `SELECT` con `INNER JOIN`, `WHERE` y `ORDER BY`, y un `INSERT` | Dos sentencias válidas | Coincide | `ejemplo2JoinEInsercion` |
| 3a | Inválido | `... WHERE semestre # 4;` | L01 (1, 46) | Coincide | `ejemplo3ErrorLexicoL01` |
| 3b | Inválido | `... WHERE nombre = 'Ana;` | L02 (1, 46) | Coincide | `ejemplo3ErrorLexicoL02` |
| 3c | Inválido | `... WHERE semestre = 3.;` | L03 (1, 48) | Coincide | `ejemplo3ErrorLexicoL03` |
| 4 | Inválido | `SELECT nombre estudiante;` y `DELETE estudiante;` | S01 (1, 15) con `',', '.' o FROM`; S01 (2, 8) con `FROM` | Coincide | `ejemplo4ErroresSintacticos` |
| 5 | Inválido | `SELECT nombr FROM estudiante WHERE semestre = 'cuarto';` | M02 (1, 8) con sugerencia «nombre»; M05 (1, 36) | Coincide | `ejemplo5ColumnaYTipos` |
| 6 | Inválido | `SELECT id FROM estudiante INNER JOIN programa ON ...;` | M03 (1, 8) | Coincide | `ejemplo6ColumnaAmbigua` |
| 7 | Inválido | `INSERT INTO estudiante (id, nombre) VALUES (1);` | M06 (1, 37) | Coincide | `ejemplo7CantidadDeValores` |
| 8 | Advertencias | `DELETE FROM estudiante;` y `SELECT * ... WHERE semestre = NULL;` | A01 (1, 1); A03 (2, 8); A02 (2, 41) | Coincide | `ejemplo8Advertencias` |

## 2. Casos nuevos del documento de diseño (clase 3)

| Caso | Tipo | Entrada | Salida esperada | Resultado | Prueba |
|---|---|---|---|---|---|
| C01 | Inválido | `SELECT nombre FROM estudiantes;` | M01 (1, 20), sugiere «estudiante» | Coincide | `c01TablaInexistente` |
| C02 | Inválido | `SELECT programa.nombre FROM estudiante;` | M04 (1, 8) | Coincide | `c02CalificadorInvalido` |
| C03 | Inválido | `INSERT INTO estudiante (id, semestre) VALUES (2, 3);` | M07 (1, 39), falta «nombre» | Coincide | `c03FaltaValorNotNull` |
| C04 | Inválido | `UPDATE estudiante SET nombre = NULL WHERE id = 1;` | M07 (1, 32) | Coincide | `c04NuloEnNotNull` |
| C05 | Inválido | `CREATE TABLE programa (id INT PRIMARY KEY);` | M08 (1, 14), la tabla ya existe | Coincide | `c05TablaRepetida` |
| C06 | Inválido | `INSERT INTO programa VALUES (3);` | M06 (1, 22), 2 columnas y 1 valor | Coincide | `c06CantidadSinLista` |
| C07 | Inválido | `INSERT INTO programa VALUES (3, '<65 caracteres>');` | M05 (1, 33), 65 caracteres y admite 60 | Coincide | `c07TextoDemasiadoLargo` |
| C08 | Inválido | `UPDATE estudiante SET semestre = 5, semestre = 6 WHERE id = 1;` | M08 (1, 37) | Coincide | `c08ColumnaRepetidaEnUpdate` |
| C09 | Inválido | `... nombre = 'Ana;` y en la línea 2 `DELETE FROM estudiante WHERE id = 1;` | L02 (1, 46); la sentencia 2 es válida | Coincide | `c09CadenaSinCerrarNoTragaLaSiguiente` |
| C10 | Inválido | `SELECT año FROM estudiante;` | L01 (1, 9) con el mensaje de tildes y ñ | Coincide | `c10LetraConTilde` |

## 3. Casos válidos adicionales

| Tipo | Entrada | Resultado esperado | Resultado | Prueba |
|---|---|---|---|---|
| Válido | `CREATE TABLE curso (codigo INT PRIMARY KEY);` y luego `SELECT codigo FROM curso;` | Sin diagnósticos; la tabla queda disponible | Coincide | `createValidoSeRegistra` |
| Válido | `select NOMBRE from ESTUDIANTE where Semestre >= 4;` | Sin diagnósticos: no distingue mayúsculas | Coincide | `sinDistinguirMayusculas` |
| Válido | `SeLeCt NoMbRe FrOm EsTuDiAnTe WhErE sEmEsTrE >= 4;` | Sin diagnósticos | Coincide | `mayusculasMezcladas` |
| Válido | `SELECT id FROM estudiante WHERE semestre >= 4.0;` | INT y DECIMAL son compatibles | Coincide | `numericosCompatibles` |
| Válido | `INSERT INTO docente VALUES (1, 'Ana', NULL, TRUE);` (esquema de la universidad) | Sin diagnósticos | Coincide | `insertValidoSinLista` |
| Válido | `DELETE FROM t WHERE (a = 1 OR b = 2) AND c IS NULL;` | Árbol con OR dentro de AND | Coincide | `parentesis` |
| Válido | `DELETE FROM t WHERE a = 1 OR b = 2 AND NOT c = 3;` | `NOT` liga más que `AND` y este más que `OR` | Coincide | `precedencia` |

## 4. Casos inválidos adicionales

| Tipo | Entrada | Resultado esperado | Resultado | Prueba |
|---|---|---|---|---|
| Sintáctico | `SELECT a FROM t` (sin `;`) | S01 al final de la sentencia, `';', INNER, WHERE u ORDER` | Coincide | `errorAlFinalDeLaSentencia` |
| Sintáctico | `CREATE TABLE t (id INT;` | S01 con `',', ')', PRIMARY o NOT` | Coincide | `errorEnCrearTabla` |
| Sintáctico | `DELETE FROM select;` | S01: palabra reservada donde iba un identificador | Coincide | `errorConPalabraReservada` |
| Semántico | `SELECT estudiante.nombr FROM estudiante;` | M02 con sugerencia, en la columna calificada | Coincide | `columnaCalificadaInexistente` |
| Semántico | `SELECT zzz FROM estudiante INNER JOIN programa ON ...;` | M02 buscando en las dos tablas | Coincide | `columnaInexistenteConJoin` |
| Semántico | `SELECT id FROM estudiante WHERE nombre = semestre;` | M05 entre dos columnas de tipos distintos | Coincide | `comparacionEntreColumnas` |
| Semántico | `INSERT INTO matricula VALUES (1, 1, 1, '2026-02-30', 4.5);` (universidad) | M05: fecha que no existe | Coincide | `fechaInvalida` |
| Semántico | `UPDATE docente SET activo = 'si' WHERE id = 1;` (universidad) | M05: cadena en una columna BOOLEAN | Coincide | `booleanConCadena` |
| Semántico | `INSERT INTO programa VALUES (NULL, 'Sistemas');` | M07: NULL en clave primaria | Coincide | `nuloEnClavePrimaria` |
| Semántico | `CREATE TABLE t (a INT PRIMARY KEY, b INT PRIMARY KEY);` | M08: dos claves primarias | Coincide | `dosClavesPrimarias` |
| Semántico | `UPDATE estudiante SET semestre = 1;` | A01 con «modificaría» | Coincide | `updateSinWhere` |
| Semántico | `SELECT estudiantes.zzz, otra FROM estudiantes ...;` | Solo M01: no se revisan las columnas de una tabla inexistente | Coincide | `tablaInexistenteNoEncadenaErrores` |

## 5. Casos límite

| Entrada | Resultado esperado | Resultado | Prueba |
|---|---|---|---|
| Texto de exactamente 60 caracteres en `VARCHAR(60)` | Válido | Coincide | `bordeDeLongitud` |
| Texto de 61 caracteres en `VARCHAR(60)` | M05 con «61 caracteres» | Coincide | `bordeDeLongitud` |
| 60 letras `ñ` en `VARCHAR(60)` | Válido: cada letra cuenta como un carácter | Coincide | `tildesCuentanComoUnCaracter` |
| `'2028-02-29'` en una columna `DATE` | Válido (año bisiesto) | Coincide | `bordesDeFecha` |
| `'2026-02-29'`, `'2026-2-9'`, `'2026-13-01'` | M05 en cada una | Coincide | `bordesDeFecha` |
| Columna mal escrita a 2 ediciones (`nmbr`) | M02 con sugerencia «nombre» | Coincide | `bordeDeSugerencia` |
| Columna mal escrita a 3 ediciones (`nmb`) | M02 sin sugerencia | Coincide | `bordeDeSugerencia` |
| Esquema y consultas vacíos, o `null` | 0 tablas y 0 sentencias, sin excepción | Coincide | `entradasVacias` |
| Consulta sin esquema | M01 y nada más | Coincide | `sinEsquema` |
| `;` suelto | S01 (1, 1) con las cinco sentencias esperadas | Coincide | `puntoYComaSuelto` |
| Saltos de línea `\r\n` y tabulaciones | Línea y columna correctas | Coincide | `crlfYTabulaciones` |
| Emoji dentro de una cadena / fuera de ella | Válido / un solo L01 | Coincide | `emoji` |
| `# ... @ ... 3.` en una sentencia | Se reportan los tres errores léxicos | Coincide | `variosErroresLexicos` |
| `VARCHAR(99999999999999999999)` | Válido, sin desbordar el entero | Coincide | `longitudGigante` |
| 200 niveles de paréntesis anidados | Válido, sin desbordar la pila | Coincide | `parentesisProfundos` |
| Condición con 3000 `AND` seguidos | Se analiza sin excepción | Coincide | `condicionLarga` |
| 1000 sentencias en un texto | 1000 resultados; la última es la sentencia 1000 en la línea 1000 | Coincide | `muchasSentencias` |
| Dos análisis seguidos con el mismo servicio | No se arrastran tablas de uno al otro | Coincide | `sinEstadoCompartido` |

## 6. Verificación del modelo formal

| Qué se verificó | Resultado | Prueba |
|---|---|---|
| La gramática tiene 35 no terminales, 48 terminales y 76 producciones | Coincide con el anexo | `GramaticaLL1Test.cantidades` |
| La tabla LL(1) no tiene celdas con más de una producción | 0 conflictos | `GramaticaLL1Test.sinConflictos` |
| Hay 15 no terminales anulables y su PRIMERO y SIGUIENTE son disjuntos | Coincide con la tabla del anexo | `anulables`, `primeroYSiguienteDisjuntos` |
| Los conjuntos SIGUIENTE calculados son los de la sección 6 del anexo | Coincide | `siguienteDelAnexo` |
| El AFD reconoce las 32 palabras reservadas sin distinguir mayúsculas | Coincide | `palabrasReservadas` |
| Cada paquete solo importa los que permite el diseño, y solo `web` usa Spring | Cumple | `ArquitecturaTest` |
