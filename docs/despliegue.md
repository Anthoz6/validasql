# Despliegue: Netlify y contenedor

## Cómo queda armado

Netlify solo aloja sitios estáticos y funciones serverless: no ejecuta aplicaciones Java ni contenedores.
Por eso ValidaSQL se despliega en dos partes:

```
Navegador ──► Netlify (index.html, app.js, estilos.css)
                 │
                 └─ /api/*  ──(reenvío)──►  Backend en un contenedor (Spring Boot, Java 21)
```

- **Netlify** publica la carpeta `src/main/resources/static` y reenvía todo lo que empieza por `/api/` al
  backend (`netlify.toml`). El navegador ve un solo origen, así que no hace falta configurar CORS.
- **El backend** corre en un contenedor Docker hecho con el `Dockerfile` del proyecto, en un servicio
  que acepte contenedores (Google Cloud Run, Render, Railway, Fly.io...).

El backend también sirve la página, así que el mismo contenedor funciona solo, sin Netlify.

## Archivos del proyecto

| Archivo | Para qué sirve |
|---|---|
| `Dockerfile` | Imagen en dos etapas: compila con el Maven Wrapper y ejecuta con un JRE 21 y un usuario sin privilegios |
| `.dockerignore` | Deja fuera de la imagen `target/`, `.git/`, `docs/` y archivos del editor |
| `docker-compose.yml` | Levanta el contenedor en local con un solo comando |
| `netlify.toml` | Carpeta a publicar, reenvío de `/api/*` y cabeceras de seguridad |
| `src/main/resources/application.properties` | Lee el puerto de la variable `PORT` (por defecto 8080) |

## Probar el contenedor en local

```bash
docker compose up --build
```

Abre <http://localhost:8080>. Sin Docker Compose:

```bash
docker build -t validasql .
docker run --rm -p 8080:8080 validasql
```

## Paso 1: desplegar el backend

La imagen no ejecuta las pruebas; corre `./mvnw test` antes de desplegar.

**Opción A: Google Cloud Run** (construye la imagen desde el `Dockerfile`):

```bash
gcloud run deploy validasql --source . --region us-central1 --allow-unauthenticated
```

Al terminar muestra la URL del servicio, parecida a `https://validasql-xxxxx-uc.a.run.app`.

**Opción B: Render o Railway.** Se crea un servicio web desde el repositorio de GitHub y se elige
«Docker» como entorno. Ambos detectan el `Dockerfile` y asignan el puerto con `PORT`.

Para comprobar que el backend responde:

```bash
curl https://URL-DEL-BACKEND/api/esquema-ejemplo
```

Los servicios gratuitos suelen apagar el contenedor sin tráfico, así que la primera petición después de un
rato puede tardar varios segundos. Antes de la defensa conviene abrir la página una vez para despertarlo.

## Paso 2: apuntar Netlify al backend

En `netlify.toml`, cambiar la línea `to` por la URL del backend:

```toml
to = "https://validasql-xxxxx-uc.a.run.app/api/:splat"
```

La URL va sin barra final y conserva `/api/:splat`. Netlify no permite variables de entorno en esta
línea, así que se escribe directamente en el archivo.

## Paso 3: publicar en Netlify

1. Subir el cambio al repositorio de GitHub.
2. En Netlify: **Add new site → Import an existing project**, elegir GitHub y el repositorio `validasql`.
3. Netlify lee `netlify.toml`: la carpeta de publicación ya está definida y no hace falta comando de
   compilación. Dejar esos campos como los propone.
4. Pulsar **Deploy**.

## Verificación

1. Abrir la URL de Netlify, pulsar **Cargar ejemplo** y comprobar que salen 11 sentencias (4 válidas,
   2 con advertencias y 5 con errores).
2. Si aparece «No se pudo analizar: La API respondió 404» o 502, la línea `to` de `netlify.toml` no apunta
   bien al backend.

## Seguridad

La API no pide autenticación y no guarda nada: cada análisis es independiente y no hay base de datos. Aun
así, es una API pública que cualquiera puede llamar. Si el servicio cobra por uso, conviene fijar un máximo
de instancias (en Cloud Run, `--max-instances 2`).
