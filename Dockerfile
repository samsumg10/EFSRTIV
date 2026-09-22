# Etapa 1: compila con Maven + JDK 21 (no hace falta tener Java ni Maven instalados)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
# La caché de ~/.m2 evita descargar todas las dependencias en cada build
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -DskipTests

# Etapa 2: solo el JRE 21 y el jar
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
