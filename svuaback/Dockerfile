# --------- Etapa 1: Build con Maven ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build

# Definir directorio de trabajo
WORKDIR /app

# Copiar el archivo pom.xml y descargar dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el resto del código fuente
COPY src ./src

# Compilar y empaquetar la aplicación
RUN mvn clean package -DskipTests

# --------- Etapa 2: Imagen final con JDK ----------
FROM eclipse-temurin:21-jdk

# Directorio de trabajo
WORKDIR /app

# Copiar el jar desde la etapa anterior
COPY --from=build /app/target/svuaback-0.0.1.jar app.jar

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]