# ValidaSQL

Analizador y validador de consultas SQL con mensajes explicativos en español.
Proyecto integrador de Compiladores, Corporación Universitaria Rafael Núñez (CURN), Cartagena.

ValidaSQL recibe un esquema (sentencias `CREATE TABLE`) y una o varias consultas, y las revisa en tres
fases escritas a mano, sin ANTLR, JSqlParser ni ninguna librería de análisis:

| Fase | Técnica | Dónde está |
|---|---|---|
| Análisis léxico | AFD de 19 estados (q0 a q18) | `lexico/AnalizadorLexico` |
| Análisis sintáctico | Descendente recursivo sobre una gramática LL(1) | `sintactico/AnalizadorSintactico` |
| Análisis semántico | Tabla de símbolos y comprobación de tipos | `semantico/AnalizadorSemantico` |

El lenguaje que acepta, los tokens, el autómata, la gramática y las reglas semánticas están definidos en
el anexo técnico (clase 2) y la arquitectura en el documento de diseño (clase 3). Los 8 ejemplos del
anexo son pruebas automáticas y dan exactamente la misma salida: código, línea, columna y mensaje.

## Requisitos

- Java 21 o superior.
- No hace falta instalar Maven: el proyecto trae el Maven Wrapper (`mvnw`).

## Cómo ejecutarlo

Todos los comandos se ejecutan dentro de la carpeta `validasql/`.

**Pruebas** (127 pruebas, incluidos los 8 ejemplos del anexo y los 10 casos nuevos):

```bash
./mvnw test
```

**Interfaz web** en <http://localhost:8080>:

```bash
./mvnw spring-boot:run
```

En la página se pegan el esquema y las consultas y se pulsa **Analizar**. **Cargar ejemplo** trae el
esquema de la universidad y unas consultas de prueba. Cada sentencia tiene una tarjeta con cuatro
pestañas: Resultado, Tokens, Árbol y Traza LL(1).

**Consola**, con el esquema y las consultas en dos archivos:

```bash
./mvnw -q exec:java -Dexec.args="src/main/resources/ejemplos/universidad.sql src/main/resources/ejemplos/consultas.sql"
```

Las opciones `--tokens`, `--arbol` y `--traza` muestran lo que produjo cada fase:

```bash
./mvnw -q exec:java -Dexec.args="--tokens --arbol --traza src/main/resources/ejemplos/universidad.sql src/main/resources/ejemplos/consultas.sql"
```

## Despliegue

El `Dockerfile` empaqueta el backend (API y página) en un contenedor, y `netlify.toml` publica la página
en Netlify reenviando `/api/*` al backend. Netlify no ejecuta Java, así que el contenedor va en otro
servicio. Los pasos están en [`docs/despliegue.md`](docs/despliegue.md).

```bash
docker compose up --build
```

## Estructura

```
src/main/java/co/edu/curn/validasql/
├─ reporte/      Diagnostico, CodigoDiagnostico (los 15 códigos y sus mensajes), FormateadorReporte
├─ lexico/       TipoToken, Token, AnalizadorLexico, Segmentador, UnidadSentencia
├─ arbol/        Sentencia y sus records, Condicion, Operando, ImpresorArbol
├─ sintactico/   AnalizadorSintactico, ConjuntosLL1 (PRIMERO y SIGUIENTE), PasoTraza
├─ semantico/    TablaSimbolos, AnalizadorSemantico, VerificadorTipos, Sugeridor
├─ servicio/     ServicioAnalisis: orquesta las fases
├─ web/          API REST con Spring Boot (único paquete que usa Spring)
└─ consola/      Consola
src/main/resources/static/    index.html, app.js, estilos.css
src/main/resources/ejemplos/  universidad.sql, consultas.sql
src/test/java/                pruebas JUnit 5
docs/                         evidencias, tabla de casos y registro de pendientes
```

Las dependencias van en un solo sentido (`reporte` no usa nada y `servicio` usa todo lo anterior).
`ArquitecturaTest` lo comprueba leyendo los `import` de cada archivo.

## API

| Ruta | Descripción |
|---|---|
| `GET /api/esquema-ejemplo` | Devuelve `{esquema, consultas}` con el esquema de la universidad |
| `POST /api/analizar` | Recibe `{esquema, consultas}` y devuelve el resumen y, por sentencia, tokens, árbol, traza y diagnósticos |

Un SQL con errores responde 200 con sus diagnósticos. Solo un cuerpo ausente responde 400.

## Formato de la salida

```
[CÓDIGO] Tipo (línea L, columna C). Mensaje.
```

Ejemplo (ejemplo 5 del anexo):

```
[M02] Error semántico (línea 1, columna 8). La columna «nombr» no existe en la tabla «estudiante». ¿Quisiste decir «nombre»?
[M05] Error semántico (línea 1, columna 36). No se puede comparar «semestre», que es INT, con una cadena de texto.
```

Línea y columna empiezan en 1. El esquema y las consultas son dos entradas separadas y cada una cuenta
sus líneas desde 1.

## Alcance

Incluye `CREATE TABLE`, `SELECT` (con un solo `INNER JOIN`, `WHERE` y `ORDER BY`), `INSERT`, `UPDATE` y
`DELETE`, con los tipos `INT`, `DECIMAL`, `VARCHAR(n)`, `DATE` y `BOOLEAN`.

No incluye subconsultas, `GROUP BY`, `HAVING`, agregaciones, alias con `AS`, `LEFT`/`RIGHT`/`FULL JOIN`,
`JOIN` de más de dos tablas, claves foráneas, `ALTER`, `DROP`, ejecución real, comentarios SQL, números
negativos ni recuperación de errores sintácticos (el parser se detiene en el primer error de cada
sentencia y sigue con la siguiente).

## Más documentación

- [`docs/evidencias-clase4.md`](docs/evidencias-clase4.md): ejecución real de los casos de la clase 4.
- [`docs/tabla-casos.md`](docs/tabla-casos.md): tabla de casos y resultados.
- [`docs/despliegue.md`](docs/despliegue.md): contenedor y Netlify.
- [`docs/pendientes.md`](docs/pendientes.md): errores conocidos, limitaciones y tareas pendientes.
