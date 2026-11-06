# Registro de pendientes

Estado al 6 de noviembre de 2026, antes de la entrega de la clase 4.

## Estado actual

El núcleo está completo: léxico, segmentador, parser LL(1) con traza, árbol, analizador semántico con
tabla de símbolos, servicio, consola, API REST y página web. Pasan las 126 pruebas, entre ellas los 8
ejemplos del anexo y los 10 casos nuevos del diseño. Es más de lo que pedía la clase 4 (solo el recorrido
de `CREATE TABLE` y `SELECT`); lo demás ya se puede usar y probar.

## Tareas pendientes

| # | Tarea | Para cuándo | Responsable |
|---|---|---|---|
| 1 | Crear el repositorio remoto y subir el proyecto (esta carpeta aún no es un repositorio Git) | Clase 4 | Juan José |
| 2 | Confirmar con la docente que el subconjunto de SQL coincide con lo que ve el grupo en Bases de Datos | Clase 4 | Grupo |
| 3 | Confirmar el reparto de responsables propuesto en el documento de diseño | Clase 4 | Grupo |
| 4 | Cada integrante lee su módulo y practica explicarlo (la defensa es individual) | Antes de la defensa | Todos |
| 5 | Informe técnico final en Word y PDF, con la portada y el estilo de la propuesta | Clase 5 | Grupo |
| 6 | Borrador de diapositivas con el guion de la sustentación | Clase 5 | Grupo |
| 7 | Actualizar el anexo y el diseño con las decisiones de implementación de la sección siguiente | Clase 5 | Grupo |
| 8 | Ensayo de la defensa con preguntas técnicas cruzadas entre integrantes | Después de la clase 5 | Todos |

## Decisiones de implementación que los documentos no definían

Al programar hubo que decidir cosas que el anexo y el diseño no especificaban. Ninguna cambia la salida de
los 8 ejemplos ni de los 10 casos nuevos, pero conviene que queden escritas en el informe final.

1. **Mensajes con variantes.** Un mismo código puede tener más de una redacción: L01 (con y sin tildes),
   M02 (una tabla o varias en un JOIN), M05 (comparación, asignación, longitud y fecha), M06 (con y sin
   lista de columnas), M07 (falta un valor o NULL) y M08 (tabla, columna, clave primaria, SET e INSERT).
   Todas están en `CodigoDiagnostico`.
2. **S01 y el final de la sentencia.** Si falta el `;`, el error se ubica justo después del último token y
   dice «se encontró el final de la sentencia». Los tokens encontrados se describen según su clase:
   identificador, número, cadena, palabra reservada o símbolo.
3. **Orden de los esperados en S01.** Salen en el orden de declaración de `TipoToken`, que es el
   mecanismo del diseño. Por eso un error tras una condición dice `';', ORDER, AND u OR` y no
   `';', AND, OR u ORDER`.
4. **M07 y nombres desconocidos.** Si la lista de columnas de un `INSERT` tiene un nombre que no existe
   (M02), no se informa M07 por las columnas NOT NULL omitidas, porque casi siempre es esa misma columna
   mal escrita.
5. **M08 en CREATE TABLE.** Además de la tabla repetida (C05), se informa la columna repetida y una
   segunda clave primaria. Una tabla con cualquiera de estos errores no se registra.
6. **A02 también con `<>`.** Comparar con NULL mediante `<>` avisa igual que con `=`. Con `<`, `>`, `<=` o
   `>=` no se avisa.
7. **Comparaciones entre literales.** `1 = 'a'` es M05; NULL se compara con todo.
8. **DATE en comparaciones.** `fecha = 'hoy'` es M05, igual que en un `INSERT`.
9. **PRIMERO y SIGUIENTE se calculan.** `ConjuntosLL1` los obtiene de la gramática con el algoritmo de
   punto fijo, en lugar de copiarlos de las tablas del anexo. `GramaticaLL1Test` los compara con el anexo.
10. **Consola con opciones.** `--tokens`, `--arbol` y `--traza` muestran lo que produjo cada fase.
11. **Dibujo del árbol.** El anexo solo muestra el de un `SELECT` simple. Para `CREATE TABLE`, `INSERT`,
    `UPDATE`, `DELETE`, `JOIN` y `ORDER BY` se siguió el mismo estilo.

## Limitaciones conocidas

- **Sugerencias con nombres muy cortos.** La regla es distancia de Levenshtein menor o igual a 2 para
  cualquier longitud, así que en una base con tablas `a` y `b`, escribir `a` donde no existe sugiere `b`.
  Con nombres de uso real no pasa; una mejora sería exigir una distancia proporcional a la longitud.
- **El esquema acepta cualquier sentencia.** No se rechaza un `INSERT` o un `SELECT` en el editor del
  esquema; se analizan igual. Solo las `CREATE TABLE` registran tablas.
- **Un solo error sintáctico por sentencia.** Es una decisión del alcance (sin recuperación de errores).
- **JOIN de una tabla consigo misma.** No hay alias, así que una columna de esa tabla se reporta como
  ambigua entre «t» y «t».
- **Columnas con emoji.** Un carácter fuera del plano básico cuenta como dos columnas.
- **La página no guarda nada.** El esquema y las consultas se pierden al recargar. No hay base de datos ni
  ejecución real de las sentencias, por alcance.
- **Sin números negativos ni comentarios SQL**, por alcance. `-1` da L01 y un comentario da L01.

## Mejora futura si sobra tiempo

- Traducir los `SELECT` válidos a álgebra relacional (unidad 3 del plan de asignatura).
- Resaltar en el editor la posición de cada diagnóstico (hoy se puede saltar a ella desde el resultado).

## Errores corregidos durante el desarrollo

- Los 14 primeros fallos de las pruebas nuevas eran columnas mal contadas a mano en las expectativas, salvo
  uno real: un `INSERT` con un nombre de columna mal escrito informaba además M07 en cascada (punto 4 de
  arriba). Ya está corregido y tiene prueba.
- En pantallas angostas la página se ensanchaba más que la ventana por una columna de grid sin
  `min-width: 0`. Corregido.
