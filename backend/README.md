# 🚀 PictoHabla Backend — API REST & Autenticación de Profesores (FASE 6.1)

Bienvenido a la infraestructura backend de **PictoHabla**, diseñada para dar soporte en línea a la gestión de aulas, profesores y sincronización futura, **manteniendo intacto el principio de que los alumnos nunca requieren inicio de sesión ni conexión a Internet para utilizar el comunicador CAA**.

---

## 🏛️ 1. Arquitectura y Principios de Diseño

El backend está construido siguiendo una arquitectura modular por capas con **Spring Boot 3.3.5** y **Java 21 LTS**, completamente desacoplada del proyecto Android.

```
backend/
├── compose.dev.yaml               # Orquestación de desarrollo con Docker Compose
├── Dockerfile                     # Imagen multi-stage optimizada para producción
├── .env.example                   # Plantilla de variables de entorno (sin secretos)
├── build.gradle.kts               # Sistema de compilación canónico (Gradle 9.3.1 con wrapper propio)
└── src/
    ├── main/
    │   ├── java/com/cmanzano/pictohabla/
    │   │   ├── config/            # Propiedades fuertemente tipadas y CORS
    │   │   ├── common/            # DTOs globales (ApiResponse), excepciones y utilidades
    │   │   ├── security/          # Spring Security 6, JWT Service, Filtros y Handlers
    │   │   ├── auth/              # Controladores, servicios y DTOs de autenticación
    │   │   ├── teacher/           # Entidad Teacher, servicio y endpoints de perfil
    │   │   ├── classroom/         # Entidad Classroom, servicio y CRUD con aislamiento
    │   │   └── student/           # Entidad Student con pseudónimos (Privacidad por diseño)
    │   └── resources/
    │       ├── application.yml    # Configuración base común
    │       ├── application-dev.yml# Perfil de desarrollo (PostgreSQL local / Docker)
    │       ├── application-prod.yml# Perfil de producción (TrueNAS / Cloudflare)
    │       └── db/migration/      # Migraciones Flyway versionadas (V1__initial_schema.sql)
    └── test/                      # Batería de pruebas unitarias y de integración
```

### 🛡️ Principios Clave:
1. **Acceso Universal para Alumnos:** Ningún niño necesita usuario, correo, contraseña ni conexión a internet para comunicarse.
2. **Aislamiento Multitenant Estricto:** Cada aula pertenece exclusivamente a su profesor creador. El servidor valida en cada operación la autoría del recurso; ningún profesor puede leer, modificar o borrar aulas de otros profesores.
3. **Privacidad Infantil por Diseño (RGPD / LOPDGDD):** Los alumnos no se registran con datos personales identificables (sin nombres y apellidos reales, sin DNI, sin teléfono, sin correo, sin fecha de nacimiento y **cero datos médicos o diagnósticos**). Se emplean códigos pseudónimos únicos (p. ej. `ALU-7B3K9A`) y apodos contextuales opcionales de aula.
4. **Almacenamiento de Secretos y Tokens:**
   - Contraseñas procesadas mediante hash **BCrypt (factor de coste 12)**.
   - Tokens de actualización (*Refresh Tokens*) persistidos exclusivamente como **hashes criptográficos SHA-256**, nunca en texto plano.
   - Rotación estricta de tokens y **detección de reutilización** (ante sospecha de robo, se invalidan inmediatamente todas las sesiones del usuario).
   - Cookies `HttpOnly`, `SameSite=Strict` y `Secure` (en producción).

---

## 🛠️ 2. Tecnologías Utilizadas

- **Lenguaje:** Java 21 LTS
- **Framework:** Spring Boot 3.5.16
- **Seguridad:** Spring Security 6.5 + JJWT (Java JWT 0.12.6)
- **Persistencia:** Spring Data JPA / Hibernate 6.5
- **Base de Datos:** PostgreSQL 16+
- **Migraciones:** Flyway Core + Flyway PostgreSQL
- **Validación:** Jakarta Bean Validation (Hibernate Validator)
- **Contenedores:** Docker & Docker Compose
- **Build Tool:** Gradle 8.12 (versión compatible con Spring Boot 3.5.16 y Java 21 LTS mediante `gradlew`)

---

## ⚙️ 3. Configuración y Variables de Entorno

Copia el archivo `.env.example` a `.env` en la carpeta `backend/`:

```bash
cp .env.example .env
```

| Variable | Descripción | Valor por defecto (Dev) |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring activo (`dev` o `prod`) | `dev` |
| `SERVER_PORT` | Puerto HTTP expuesto por la API | `8080` |
| `DB_HOST` | Host de PostgreSQL | `localhost` (o `postgres` en Docker) |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_NAME` | Nombre de la base de datos | `pictohabla` |
| `DB_USER` | Usuario de PostgreSQL | `pictohabla` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `pictohabla_secret_change_in_prod` |
| `JWT_SECRET` | Clave secreta HMAC-SHA256 (mínimo 256 bits) | *(clave hexadecimal de desarrollo)* |
| `JWT_ACCESS_EXPIRATION_MINUTES` | Tiempo de vida del Access Token | `15` |
| `JWT_REFRESH_EXPIRATION_DAYS` | Tiempo de vida del Refresh Token | `7` |
| `SECURITY_COOKIE_SECURE` | Marcar cookies como `Secure` (HTTPS) | `false` en dev, `true` en prod |
| `EMAIL_VERIFICATION_REQUIRED` | Obligatoriedad de verificar correo antes de login | `false` en dev, `true` en prod |
| `CORS_ALLOWED_ORIGINS` | Orígenes web permitidos | `http://localhost:3000,http://localhost:5173` |

---

## 🚀 4. Puesta en Marcha

### Opción A: Con Docker Compose (Recomendada para desarrollo)

Levanta la base de datos PostgreSQL y la API en contenedores aislados:

```bash
cd backend
docker compose -f compose.dev.yaml up -d
```

Para consultar los logs:
```bash
docker compose -f compose.dev.yaml logs -f backend
```

Para detener los servicios conservando los datos del volumen:
```bash
docker compose -f compose.dev.yaml down
```

### Opción B: Con PostgreSQL Local / Nativo y Gradle

1. Asegúrate de tener PostgreSQL ejecutándose en el puerto 5432 con la base de datos creada:
   ```sql
   CREATE DATABASE pictohabla;
   CREATE USER pictohabla WITH ENCRYPTED PASSWORD 'pictohabla_secret_change_in_prod';
   GRANT ALL PRIVILEGES ON DATABASE pictohabla TO pictohabla;
   ```
2. Ejecuta la aplicación mediante el Gradle Wrapper propio del backend:
   ```bash
   cd backend
   ./gradlew bootRun
   ```
   *(En Windows: `.\gradlew.bat bootRun`)*

---

## 🧪 5. Pruebas Automatizadas

El proyecto cuenta con una suite completa de pruebas unitarias y de integración que verifican seguridad, migraciones Flyway, autenticación, control de accesos multitenant y validación de DTOs.

Para ejecutar los tests:

```bash
cd backend
./gradlew test
```
*(En Windows: `.\gradlew.bat test`)*

Para empaquetar el ejecutable de producción:
```bash
./gradlew bootJar
```
El archivo JAR autoejecutable se generará en `build/libs/pictohabla-backend-0.0.1-SNAPSHOT.jar`.

---

## 📡 6. Catálogo de Endpoints de la API (`/api/v1`)

Todos los endpoints devuelven respuestas estandarizadas bajo el formato `ApiResponse<T>`:
```json
{
  "success": true,
  "message": "Operación exitosa",
  "data": { ... },
  "timestamp": "2026-10-30T10:00:00Z"
}
```

### 🔐 Autenticación (`/api/v1/auth`)

#### 1. Registro de Profesor
- **Método:** `POST /api/v1/auth/register`
- **Público:** Sí
- **Cuerpo:**
  ```json
  {
    "email": "profesora.laura@colegio.es",
    "password": "PasswordSegura123",
    "fullName": "Laura Gómez"
  }
  ```
- **Respuesta (201 Created):**
  ```json
  {
    "success": true,
    "message": "Profesor registrado exitosamente",
    "data": {
      "id": "c1f7287e-3559-4f76-80f4-5f53e390cbf1",
      "email": "profesora.laura@colegio.es",
      "fullName": "Laura Gómez",
      "status": "ACTIVE",
      "emailVerified": true,
      "createdAt": "2026-10-30T10:00:00Z"
    }
  }
  ```

#### 2. Inicio de Sesión
- **Método:** `POST /api/v1/auth/login`
- **Público:** Sí
- **Cuerpo:**
  ```json
  {
    "email": "profesora.laura@colegio.es",
    "password": "PasswordSegura123"
  }
  ```
- **Respuesta (200 OK):** Devuelve el JWT en el cuerpo e inyecta la cookie `pictohabla_refresh` HttpOnly.
  ```json
  {
    "success": true,
    "message": "Inicio de sesión exitoso",
    "data": {
      "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
      "tokenType": "Bearer",
      "expiresInSeconds": 900,
      "teacher": {
        "id": "c1f7287e-3559-4f76-80f4-5f53e390cbf1",
        "email": "profesora.laura@colegio.es",
        "fullName": "Laura Gómez"
      },
      "refreshToken": "7c88b776-90f7-4dc1-bbbb-03bdfef2bc74"
    }
  }
  ```

#### 3. Renovación de Sesión (Token Refresh con Rotación)
- **Método:** `POST /api/v1/auth/refresh`
- **Público:** Sí (valida la cookie o el cuerpo opcional `{"refreshToken": "..."}`)
- **Respuesta (200 OK):** Nuevo Access Token y nuevo Refresh Token rotado.

#### 4. Cierre de Sesión
- **Método:** `POST /api/v1/auth/logout`
- **Público:** Sí (invalida el refresh token en base de datos y borra la cookie)

#### 5. Solicitud de Restablecimiento de Contraseña
- **Método:** `POST /api/v1/auth/forgot-password`
- **Cuerpo:** `{"email": "profesora.laura@colegio.es"}`
- **Respuesta (200 OK):** Mensaje neutro de confirmación sin revelar si la cuenta existe.

#### 6. Restablecimiento de Contraseña
- **Método:** `POST /api/v1/auth/reset-password`
- **Cuerpo:**
  ```json
  {
    "token": "reset-token-uuid-recibido",
    "newPassword": "NuevaPassword123"
  }
  ```

#### 7. Perfil del Profesor Autenticado
- **Método:** `GET /api/v1/auth/me`
- **Encabezado:** `Authorization: Bearer <accessToken>`

---

### 🏫 Gestión de Aulas (`/api/v1/classrooms`)

*Todos los endpoints requieren `Authorization: Bearer <accessToken>`.*

#### 1. Crear Aula
- **Método:** `POST /api/v1/classrooms`
- **Cuerpo:**
  ```json
  {
    "name": "Aula 3º Primaria B",
    "description": "Aula de integración y apoyo a la comunicación"
  }
  ```

#### 2. Listar Mis Aulas
- **Método:** `GET /api/v1/classrooms`
- **Respuesta (200 OK):** Lista únicamente las aulas activas del profesor autenticado.

#### 3. Obtener Detalle de Aula
- **Método:** `GET /api/v1/classrooms/{id}`
- **Seguridad:** Retorna `404 Not Found` si el aula pertenece a otro profesor.

#### 4. Actualizar Aula
- **Método:** `PATCH /api/v1/classrooms/{id}`
- **Cuerpo:**
  ```json
  {
    "name": "Aula 3º Primaria B (Actualizada)",
    "description": "Nueva descripción"
  }
  ```

#### 5. Eliminar Aula (Desactivación Lógica Segura)
- **Método:** `DELETE /api/v1/classrooms/{id}`

---

### 👦 Alumnos y Pseudónimos (`/api/v1/classrooms/{classroomId}/students`)

#### 1. Incorporar Alumno al Aula
- **Método:** `POST /api/v1/classrooms/{classroomId}/students`
- **Cuerpo:**
  ```json
  {
    "nickname": "Lucas"
  }
  ```
- **Respuesta (201 Created):** Genera automáticamente un código pseudónimo único y seguro.
  ```json
  {
    "success": true,
    "message": "Alumno agregado exitosamente con código pseudónimo",
    "data": {
      "id": "e93297a7-15ef-4d14-87f5-d5b78f8cb080",
      "classroomId": "b8109bf5-25b4-4e20-9cb5-ef561fae4895",
      "pseudonymCode": "ALU-9F2B8C",
      "nickname": "Lucas",
      "isActive": true
    }
  }
  ```

#### 2. Listar Alumnos del Aula
- **Método:** `GET /api/v1/classrooms/{classroomId}/students`

---

## 🔒 7. Consideraciones de Privacidad y Normativa (RGPD / LOPDGDD)

- **Minimización de Datos (Art. 5.1.c RGPD):** No se almacenan historiales de frases de comunicación de los alumnos en esta fase, ni información de salud o diagnósticos médicos.
- **Seudonimización (Art. 4.5 RGPD):** Los alumnos se identifican en el sistema central únicamente a través del código alfanumérico generado (`pseudonymCode`). La asociación entre el código y la persona física permanece exclusivamente en el contexto presencial del educador/tutor.
- **Auditoría Pre-Producción:** Antes de abrir el servicio a colegios o instituciones públicas, se requerirá formalizar el Registro de Actividades de Tratamiento (RAT) y la designación/comunicación del Delegado de Protección de Datos (DPD) correspondiente a la entidad responsable.

---

## 🖥️ 8. Plan de Despliegue en TrueNAS SCALE

El despliegue final en producción se llevará a cabo en el servidor TrueNAS SCALE con las siguientes pautas:

- **Hardware:** Intel Xeon E3-1225 v6, 32 GB RAM ECC.
- **Datasets de Almacenamiento Reales en TrueNAS:**
  - `Datos/PictoHabla/Postgres`: Almacenamiento persistente de PostgreSQL (`/mnt/Datos/PictoHabla/Postgres`, UID 70:70, recordsize=16k).
  - `Datos/PictoHabla/Uploads`: Archivos multimedia y pictogramas (`/mnt/Datos/PictoHabla/Uploads`, UID 1000:1000, recordsize=128k).
  - `Seguridad/PictoHabla/Backups`: Volcados lógicos periódicos (`/mnt/Seguridad/PictoHabla/Backups`, UID 0:0, 700).
  - Scripts de soporte: `/mnt/Datos/PictoHabla/scripts/` (`backup.sh` y `restore.sh`).
- **Conectividad:**
  - Sin exposición pública de PostgreSQL (aislado en red interna Docker `pictohabla-net`).
  - Publicación externa exclusiva mediante **Cloudflare Tunnel (`cloudflared`)** con binding restringido a loopback (`127.0.0.1:18080`).
- **Límites de Recursos:**
  - PostgreSQL: 256 MB RAM
  - Spring Boot API: 512 MB RAM (JVM con `-XX:MaxRAMPercentage=75.0`)
  *(Garantiza un consumo inferior a 800 MB, conviviendo holgadamente con los ~3 GiB de memoria disponibles en el host).*
