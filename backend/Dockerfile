# ========================================================
# Etapa 1: Construcción (Build Stage)
# ========================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copiar archivos de construcción primero para cachear dependencias
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# Copiar código fuente y compilar
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ========================================================
# Etapa 2: Imagen de Ejecución (Runtime Stage)
# ========================================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="PictoHabla Team"
LABEL description="PictoHabla Backend REST API - TrueNAS SCALE Production"

# Instalar curl para healthchecks nativos y diagnóstico seguro
RUN apk add --no-cache curl tzdata

# Crear directorio de aplicación y subidas multimedia
WORKDIR /app

# Crear usuario y grupo de sistema sin privilegios con UID/GID fijo 1000 (Determinismo para ACLs en TrueNAS)
RUN addgroup -g 1000 -S spring && adduser -u 1000 -S spring -G spring && \
    mkdir -p /app/uploads && \
    chown -R spring:spring /app

# Copiar artefacto compilado desde la etapa anterior con permisos no-root
COPY --chown=spring:spring --from=builder /build/build/libs/pictohabla-backend-*.jar app.jar

USER spring:spring

EXPOSE 8080

# Opciones de JVM optimizadas para contenedores con memoria acotada (Xeon TrueNAS / Docker)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
