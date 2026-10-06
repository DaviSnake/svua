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

# 🔒 Se extrae el jar a un layout plano (jar fino + lib/): la libreria nativa
# 7-Zip-JBinding (respaldo cifrado .7z) busca sus clases con el classloader
# del sistema, que NO ve los jars anidados de un fat jar de Spring Boot, y
# tumba la JVM entera con "Class ... IInStream not found".
RUN java -Djarmode=tools -jar target/svuaback-0.0.1.jar extract --destination /app/extracted

# --------- Etapa 2: Imagen final con JDK ----------
FROM eclipse-temurin:21-jdk

# 🔥 Zona horaria Chile
ENV TZ=America/Santiago

RUN apt-get update && apt-get install -y tzdata \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*

# Directorio de trabajo
WORKDIR /app

# Copiar el jar extraido (svuaback-0.0.1.jar + lib/) desde la etapa anterior
COPY --from=build /app/extracted /app

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "svuaback-0.0.1.jar"]