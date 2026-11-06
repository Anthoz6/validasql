# Evidencias de la clase 4: primer avance funcional

Este documento muestra el recorrido completo de ValidaSQL (análisis léxico, sintáctico y semántico) sobre
cuatro casos del anexo técnico de la clase 2. Las salidas se copiaron tal cual las imprime la consola y
son las mismas que dan los ejemplos del anexo: mismo código, misma línea, misma columna y mismo mensaje.

Todos usan el esquema común del anexo (`esquema_comun.sql`):

```sql
CREATE TABLE programa (id INT PRIMARY KEY, nombre VARCHAR(60) NOT NULL);
CREATE TABLE estudiante (id INT PRIMARY KEY, nombre VARCHAR(60) NOT NULL,
                         semestre INT, id_programa INT);
```

Estos casos, y el resto de los ejemplos del anexo, también son pruebas automáticas
(`EjemplosAnexoTest`) que comparan la salida completa. Se ejecutan con `./mvnw test`.

## Caso 1. Consulta válida (ejemplo 1 del anexo)

Recorre las tres fases sin errores. Con `--tokens --arbol --traza` se ve lo que produjo cada una.

Entrada (`caso1.sql`):

```sql
SELECT nombre FROM estudiante WHERE semestre >= 4;
```

Comando:

```bash
./mvnw -q exec:java -Dexec.args="--tokens --arbol --traza esquema_comun.sql caso1.sql"
```

Salida real:

```text
Esquema: 2 tablas cargadas (programa, estudiante). Sin errores.

Sentencia 1 (línea 1): SELECT nombre FROM estudiante WHERE semestre >= 4;
  Tokens:
    SELECT     SELECT         línea 1, columna 1
    IDENT      nombre         línea 1, columna 8
    FROM       FROM           línea 1, columna 15
    IDENT      estudiante     línea 1, columna 20
    WHERE      WHERE          línea 1, columna 31
    IDENT      semestre       línea 1, columna 37
    MAYORIG    >=             línea 1, columna 46
    NUM_ENT    4              línea 1, columna 49
    PUNTOCOMA  ;              línea 1, columna 50
  Árbol:
    Select
    ├─ columnas: Col(nombre)
    ├─ FROM: estudiante
    └─ WHERE
       └─ Comparación (>=)
          ├─ Col(semestre)
          └─ NUM_ENT(4)
  Traza LL(1):
     1  Sent     SELECT       Sent → Select ';'                                PRIMERO
     2  Select   SELECT       Select → SELECT Cols FROM IDENT Join Where Orden PRIMERO
     3  Cols     nombre       Cols → Col ColsR                                 PRIMERO
     4  Col      nombre       Col → IDENT ColQ                                 PRIMERO
     5  ColQ     FROM         ColQ → ε                                         SIGUIENTE
     6  ColsR    FROM         ColsR → ε                                        SIGUIENTE
     7  Join     WHERE        Join → ε                                         SIGUIENTE
     8  Where    WHERE        Where → WHERE Cond                               PRIMERO
     9  Cond     semestre     Cond → CondY CondO                               PRIMERO
    10  CondY    semestre     CondY → CondN CondYR                             PRIMERO
    11  CondN    semestre     CondN → Prim                                     PRIMERO
    12  Prim     semestre     Prim → Opnd PrimR                                PRIMERO
    13  Opnd     semestre     Opnd → Col                                       PRIMERO
    14  Col      semestre     Col → IDENT ColQ                                 PRIMERO
    15  ColQ     >=           ColQ → ε                                         SIGUIENTE
    16  PrimR    >=           PrimR → OpRel Opnd                               PRIMERO
    17  OpRel    >=           OpRel → '>='                                     PRIMERO
    18  Opnd     4            Opnd → Lit                                       PRIMERO
    19  Lit      4            Lit → NUM_ENT                                    PRIMERO
    20  CondYR   ;            CondYR → ε                                       SIGUIENTE
    21  CondO    ;            CondO → ε                                        SIGUIENTE
    22  Orden    ;            Orden → ε                                        SIGUIENTE
  Sentencia válida. Sin errores ni advertencias.

Resumen: sentencias 1, válidas 1, con advertencias 0, con errores 0.
```

## Caso 2. Errores léxicos (ejemplo 3 del anexo)

Las tres sentencias van en líneas separadas del archivo, por eso cada una informa su propia línea. Con un error léxico la sentencia no pasa al parser, y la cadena sin cerrar (L02) no se traga la línea siguiente.

Entrada (`caso2.sql`):

```sql
SELECT nombre FROM estudiante WHERE semestre # 4;
SELECT nombre FROM estudiante WHERE nombre = 'Ana;
SELECT nombre FROM estudiante WHERE semestre = 3.;
```

Comando:

```bash
./mvnw -q exec:java -Dexec.args="esquema_comun.sql caso2.sql"
```

Salida real:

```text
Esquema: 2 tablas cargadas (programa, estudiante). Sin errores.

Sentencia 1 (línea 1): SELECT nombre FROM estudiante WHERE semestre # 4;
  [L01] Error léxico (línea 1, columna 46). Carácter no válido «#»: no pertenece al alfabeto del lenguaje.

Sentencia 2 (línea 2): SELECT nombre FROM estudiante WHERE nombre = 'Ana;
  [L02] Error léxico (línea 2, columna 46). Cadena sin cerrar: falta la comilla simple de cierre antes del fin de la línea.

Sentencia 3 (línea 3): SELECT nombre FROM estudiante WHERE semestre = 3.;
  [L03] Error léxico (línea 3, columna 48). Número decimal incompleto: después del punto se esperaba un dígito.

Resumen: sentencias 3, válidas 0, con advertencias 0, con errores 3.
```

## Caso 3. Errores semánticos: columna inexistente y tipos (ejemplo 5 del anexo)

La sentencia es válida léxica y sintácticamente, así que llega al analizador semántico, que reporta los dos errores en una sola pasada.

Entrada (`caso3.sql`):

```sql
SELECT nombr FROM estudiante WHERE semestre = 'cuarto';
```

Comando:

```bash
./mvnw -q exec:java -Dexec.args="--arbol --traza esquema_comun.sql caso3.sql"
```

Salida real:

```text
Esquema: 2 tablas cargadas (programa, estudiante). Sin errores.

Sentencia 1 (línea 1): SELECT nombr FROM estudiante WHERE semestre = 'cuarto';
  Árbol:
    Select
    ├─ columnas: Col(nombr)
    ├─ FROM: estudiante
    └─ WHERE
       └─ Comparación (=)
          ├─ Col(semestre)
          └─ CADENA('cuarto')
  Traza LL(1):
     1  Sent     SELECT       Sent → Select ';'                                PRIMERO
     2  Select   SELECT       Select → SELECT Cols FROM IDENT Join Where Orden PRIMERO
     3  Cols     nombr        Cols → Col ColsR                                 PRIMERO
     4  Col      nombr        Col → IDENT ColQ                                 PRIMERO
     5  ColQ     FROM         ColQ → ε                                         SIGUIENTE
     6  ColsR    FROM         ColsR → ε                                        SIGUIENTE
     7  Join     WHERE        Join → ε                                         SIGUIENTE
     8  Where    WHERE        Where → WHERE Cond                               PRIMERO
     9  Cond     semestre     Cond → CondY CondO                               PRIMERO
    10  CondY    semestre     CondY → CondN CondYR                             PRIMERO
    11  CondN    semestre     CondN → Prim                                     PRIMERO
    12  Prim     semestre     Prim → Opnd PrimR                                PRIMERO
    13  Opnd     semestre     Opnd → Col                                       PRIMERO
    14  Col      semestre     Col → IDENT ColQ                                 PRIMERO
    15  ColQ     =            ColQ → ε                                         SIGUIENTE
    16  PrimR    =            PrimR → OpRel Opnd                               PRIMERO
    17  OpRel    =            OpRel → '='                                      PRIMERO
    18  Opnd     'cuarto'     Opnd → Lit                                       PRIMERO
    19  Lit      'cuarto'     Lit → CADENA                                     PRIMERO
    20  CondYR   ;            CondYR → ε                                       SIGUIENTE
    21  CondO    ;            CondO → ε                                        SIGUIENTE
    22  Orden    ;            Orden → ε                                        SIGUIENTE
  [M02] Error semántico (línea 1, columna 8). La columna «nombr» no existe en la tabla «estudiante». ¿Quisiste decir «nombre»?
  [M05] Error semántico (línea 1, columna 36). No se puede comparar «semestre», que es INT, con una cadena de texto.

Resumen: sentencias 1, válidas 0, con advertencias 0, con errores 1.
```

## Caso 4. Error semántico: columna ambigua en un JOIN (ejemplo 6 del anexo)

`id` existe en las dos tablas del JOIN y no está calificada.

Entrada (`caso4.sql`):

```sql
SELECT id FROM estudiante INNER JOIN programa ON estudiante.id_programa = programa.id;
```

Comando:

```bash
./mvnw -q exec:java -Dexec.args="--arbol esquema_comun.sql caso4.sql"
```

Salida real:

```text
Esquema: 2 tablas cargadas (programa, estudiante). Sin errores.

Sentencia 1 (línea 1): SELECT id FROM estudiante INNER JOIN programa ON estudiante.id_programa = programa.id;
  Árbol:
    Select
    ├─ columnas: Col(id)
    ├─ FROM: estudiante
    └─ INNER JOIN: programa
       └─ ON
          └─ Comparación (=)
             ├─ Col(estudiante.id_programa)
             └─ Col(programa.id)
  [M03] Error semántico (línea 1, columna 8). La columna «id» es ambigua: existe en «estudiante» y «programa». Califícala, por ejemplo «estudiante.id».

Resumen: sentencias 1, válidas 0, con advertencias 0, con errores 1.
```

## Cómo repetir las evidencias

Desde la carpeta `validasql/`, con el esquema y la consulta de cada caso guardados en dos archivos:

```bash
./mvnw -q exec:java -Dexec.args="--tokens --arbol --traza esquema_comun.sql caso1.sql"
```

También se pueden ver en la página web (`./mvnw spring-boot:run`, en <http://localhost:8080>), donde cada
sentencia tiene las pestañas Resultado, Tokens, Árbol y Traza LL(1).
