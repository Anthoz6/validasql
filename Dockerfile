# ValidaSQL: imagen del backend (API REST y página web) en dos etapas.
# Construir:  docker build -t validasql .
# Ejecutar:   docker run --rm -p 8080:8080 validasql

# Etapa 1: compilar y empaquetar con el Maven Wrapper del proyecto.
FROM eclipse-temurin:21-jdk AS construccion
WORKDIR /app

# Primero solo las dependencias, para que Docker reutilice esta capa si no cambia el pom.xml.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Las pruebas se ejecutan antes, con ./mvnw test, no en la construcción de la imagen.
COPY src src
RUN ./mvnw -B -q -DskipTests package

# Etapa 2: solo el JRE y el jar, con un usuario sin privilegios.
FROM eclipse-temurin:21-jre
RUN useradd --system --create-home validasql
WORKDIR /app
COPY --from=construccion /app/target/validasql-*.jar app.jar
USER validasql

# Las plataformas (Cloud Run, Render, Railway...) definen PORT; Spring lo lee en application.properties.
ENV PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
