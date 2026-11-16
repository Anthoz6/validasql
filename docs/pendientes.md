# Registro de pendientes

Estado al 16 de noviembre de 2026, para la entrega de la clase 5.

## Estado actual

El núcleo está completo: léxico, segmentador, parser LL(1) con traza, árbol, analizador semántico con
tabla de símbolos, servicio, consola, API REST y página web. Pasan las 127 pruebas, entre ellas los 8
ejemplos del anexo y los 10 casos nuevos del diseño, con la salida exacta.

El repositorio está en GitHub (`Anthoz6/validasql`). Los entregables de la clase 4 (README, evidencias y este
registro) y los de la clase 5 (versión candidata, informe técnico final, tabla de casos y borrador de
diapositivas) están hechos. Quedan las tareas de la tabla marcadas como pendientes, que son de revisión y de
preparación de la sustentación.

## Tareas pendientes

| # | Tarea | Para cuándo | Responsable | Estado |
|---|---|---|---|---|
| 1 | Crear el repositorio remoto y subir el proyecto | Clase 4 | Juan José | Hecha |
| 2 | Confirmar con la docente que el subconjunto de SQL coincide con lo que ve el grupo en Bases de Datos | Clase 4 | Grupo | Pendiente |
| 3 | Confirmar el reparto de responsables propuesto en el documento de diseño | Clase 4 | Grupo | Pendiente |
| 4 | Cada integrante lee su módulo y practica explicarlo (la defensa es individual) | Antes de la defensa | Todos | Pendiente |
| 5 | Informe técnico final en Word y PDF, con la portada y el estilo de la propuesta | Clase 5 | Grupo | Hecha |
| 6 | Borrador de diapositivas con el guion de la sustentación | Clase 5 | Grupo | Hecha |
| 7 | Tabla de casos y resultados (`docs/tabla-casos.md` y su versión en Word y PDF) | Clase 5 | Grupo | Hecha |
| 8 | Dar acceso a la docente al repositorio, que es privado | Clase 5 | Grupo | Por confirmar |
| 9 | Actualizar el anexo y el diseño con las decisiones de implementación de la sección siguiente | Después de la clase 5 | Grupo | Pendiente |
| 10 | Ensayo de la defensa con preguntas técnicas cruzadas entre integrantes | Después de la clase 5 | Todos | Pendiente |
| 11 | Desplegar el backend y la página para mostrarlos en línea, según `docs/despliegue.md` (opcional) | Antes de la sustentación | Juan José | Pendiente |

## Decisiones de implementación que los documentos no definían

Al programar hubo que decidir cosas que el anexo y el diseño no especificaban. Ninguna cambia la salida de
los 8 ejemplos ni de los 10 casos nuevos, y ya están escritas en el informe final (sección 9).

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
12. **Diferencias con los diagramas del diseño.** `Orden` guarda `Token direccion` y no un `boolean`;
    `Actualizacion` y `Eliminacion` guardan `update` y `delete`; `ResultadoSentencia` no tiene `origen`;
    `ResultadoAnalisis` guarda `nombresTablas`; `Diagnostico` no tiene `comoTexto()` (el formato lo da
    `FormateadorReporte`); la primera pestaña de la web se llama «Resultado». El informe las explica en la
    sección 9.

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
- **El resaltado de colores es solo presentación.** La página usa su propia lista de las 32 palabras
  reservadas para colorear mientras se escribe. `PaginaWebTest` comprueba que coincida con `TipoToken`.
- **La página no guarda nada.** El esquema y las consultas se pierden al recargar. No hay base de datos ni
  ejecución real de las sentencias, por alcance.
- **Sin números negativos ni comentarios SQL**, por alcance. `-1` da L01 y un comentario da L01.

## Mejora futura si sobra tiempo

- Traducir los `SELECT` válidos a álgebra relacional (unidad 3 del plan de asignatura).
- Resaltar en el editor la posición de cada diagnóstico (hoy se puede saltar a ella desde el resultado).

## Errores corregidos durante el desarrollo

- De los 14 primeros fallos de las pruebas nuevas, 13 eran errores nuestros al escribir lo esperado
  (columnas mal contadas a mano, el orden de los tokens esperados y el nombre de un conjunto) y uno era real: un `INSERT` con un nombre de columna mal escrito informaba además M07 en cascada (punto 4 de
  arriba). Ya está corregido y tiene prueba.
- En pantallas angostas la página se ensanchaba más que la ventana por una columna de grid sin
  `min-width: 0`. Corregido.
