# 🚀 Guía Definitiva de Despliegue en Producción — TrueNAS SCALE Community 25.10.7

Esta guía documenta el procedimiento oficial, probado y seguro para desplegar el backend de **PictoHabla** y su base de datos **PostgreSQL 16** en **TrueNAS SCALE Community 25.10.7**, integrándose con la infraestructura existente de **Cloudflare Tunnel** (`ix-cloudflared_default`) sin publicar puertos en el host del servidor y manteniendo intactas las demás aplicaciones en producción.

---

## 📋 1. Especificaciones de Infraestructura y Coexistencia

### Hardware del Servidor
- **Procesador:** Intel Xeon E3-1225 v6 (arquitectura `x86_64` / `amd64`, 4 núcleos).
- **Memoria RAM:** 32 GB DDR4 ECC.
- **Asignación de Memoria PictoHabla:** 768 MB en total:
  - PostgreSQL 16: `256 MB` (límite estricto cgroup).
  - Backend Spring Boot: `512 MB` (límite estricto cgroup, 384 MB Heap máx).
- **Almacenamiento ZFS:** Dos pools independientes (`Datos` y `Seguridad`).
  > ⚠️ **Aviso de Resiliencia ZFS:** Los pools están formados por discos físicos individuales sin espejo (mirror). Las copias de seguridad lógicas automatizadas en el pool `Seguridad` son críticas para evitar pérdida de datos ante fallo mecánico del pool `Datos`. Se aconseja programar una tarea Cloud Sync hacia almacenamiento externo.

### Aplicaciones Coexistentes (Intactas)
El servidor aloja aplicaciones que **NO se modifican ni se reinician**:
- `cloudflared` (Contenedor `ix-cloudflared-cloudflared-1`, red `ix-cloudflared_default`, subred `172.16.1.0/24`, IP `172.16.1.2`).
- `tailscale`, `pihole`, `beszel`, `immich`, `vaultwarden`, `n8n`.

---

## 📁 2. Estructura Canónica de Directorios y Datasets

Toda la operativa de PictoHabla se centraliza bajo una estructura canónica limpia y unificada en el pool `Datos`, separando el almacenamiento de copias en el pool `Seguridad`. El repositorio del proyecto se clona en `source/backend`:

```
/mnt/Datos/Aplicaciones/PictoHabla/
├── .env                          # Variables de entorno de producción (chmod 600)
├── scripts/                      # Scripts de mantenimiento y auditoría (chmod 700 / 755)
│   ├── backup.sh                 # Backup diario automático (PostgreSQL custom dump)
│   ├── restore.sh                # Restauración asistida con confirmación
│   ├── precheck-truenas.sh       # Auditoría de sólo lectura previa al despliegue
│   └── bootstrap-admin.sh        # Creación interactiva y segura del primer administrador
├── source/                       # Repositorio clonado de PictoHabla
│   └── backend/                  # Módulo Spring Boot
│       ├── Dockerfile            # Multi-stage Dockerfile no-root (exec java, pictohabla-backend.jar)
│       ├── compose.truenas.yaml  # Orquestación canónica de Docker Compose para TrueNAS
│       ├── .dockerignore         # Exclusión de binarios, logs y secretos en contexto Docker
│       ├── build.gradle.kts      # Definición de dependencias y plugins (jar deshabilitado)
│       ├── settings.gradle.kts   # Configuración de repositorios y módulos
│       ├── gradlew               # Script wrapper de Gradle (chmod +x)
│       ├── gradle/
│       │   └── wrapper/
│       │       ├── gradle-wrapper.jar
│       │       └── gradle-wrapper.properties
│       └── src/                  # Código fuente y recursos de backend/
├── Postgres/                     # Dataset ZFS Datos/Aplicaciones/PictoHabla/Postgres (UID 70:70, perm 700)
└── Uploads/                      # Dataset ZFS Datos/Aplicaciones/PictoHabla/Uploads (UID 1000:1000, perm 750)

/mnt/Seguridad/PictoHabla/
└── Backups/                      # Dataset ZFS Seguridad/PictoHabla/Backups (root:root, perm 700)
```

### Ubicación de Componentes Clave:
| Elemento | Ruta en TrueNAS SCALE | Permisos Recomendados |
| :--- | :--- | :--- |
| **Directorio de Trabajo** | `/mnt/Datos/Aplicaciones/PictoHabla` | `755` (`root:root`) |
| **Orquestación Compose** | `/mnt/Datos/Aplicaciones/PictoHabla/source/backend/compose.truenas.yaml` | `644` (`root:root`) |
| **Configuración Secreta** | `/mnt/Datos/Aplicaciones/PictoHabla/.env` | `600` (`root:root`) |
| **Scripts de Operación** | `/mnt/Datos/Aplicaciones/PictoHabla/scripts/*.sh` | `700` (`root:root`) |
| **Dockerfile** | `/mnt/Datos/Aplicaciones/PictoHabla/source/backend/Dockerfile` | `644` (`root:root`) |
| **Dataset BD Postgres** | `/mnt/Datos/Aplicaciones/PictoHabla/Postgres` | `700` (`70:70` - `postgres:postgres`) |
| **Dataset Subidas Multimedia** | `/mnt/Datos/Aplicaciones/PictoHabla/Uploads` | `750` (`1000:1000` - `spring:spring`) |
| **Dataset Copias Seguridad** | `/mnt/Seguridad/PictoHabla/Backups` | `700` (`0:0` - `root:root`) |
| **Imagen Docker generada** | Daemon local de Docker (`pictohabla-backend:1.0.2`) | N/A (Docker Image Store) |

---

## 🔒 3. Auditoría de ACLs y Permisos POSIX en TrueNAS SCALE

> ⚠️ **ADVERTENCIA CRÍTICA DE ACL EN TRUENAS:** Antes de aplicar cambios de UID, GID o permisos POSIX (`chown`/`chmod`), es **obligatorio verificar el tipo de ACL** de cada dataset. TrueNAS SCALE soporta ACLs avanzadas tipo NFSv4/SMB. Si un dataset se creó con un perfil SMB o ACLs NFSv4 activas, los comandos POSIX estándar fallarán o serán sobreescritos por las ACEs (Access Control Entries) heredadas, impidiendo que PostgreSQL o Spring Boot inicien.

### Tabla de Auditoría de Datasets y ACLs:

| Dataset | Usuario esperado | UID:GID | Permisos necesarios | Tipo ACL a comprobar | Qué hacer si existe una ACL incompatible |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`Datos/Aplicaciones/PictoHabla/Postgres`** | `postgres` | `70:70` | `700` (`rwx------`) | POSIX (`acltype=posix`, `aclmode=discard`) | Si tiene ACL NFSv4 o entradas ACE heredadas, en la UI de TrueNAS ir a **Datasets** -> seleccionar `Postgres` -> **Edit Permissions** -> seleccionar perfil **POSIX Standard** y marcar **Strip ACLs**. Vía CLI: `setfacl -b /mnt/Datos/Aplicaciones/PictoHabla/Postgres && chown 70:70 /mnt/Datos/Aplicaciones/PictoHabla/Postgres && chmod 700 /mnt/Datos/Aplicaciones/PictoHabla/Postgres`. |
| **`Datos/Aplicaciones/PictoHabla/Uploads`** | `spring` | `1000:1000` | `750` (`rwxr-x---`) | POSIX (`acltype=posix`, `aclmode=discard`) | Si una ACL restrictiva bloquea a UID 1000, cambiar tipo a POSIX o añadir entrada ACE para UID 1000 (`rwx`). Vía CLI: `setfacl -b /mnt/Datos/Aplicaciones/PictoHabla/Uploads && chown 1000:1000 /mnt/Datos/Aplicaciones/PictoHabla/Uploads && chmod 750 /mnt/Datos/Aplicaciones/PictoHabla/Uploads`. |
| **`Seguridad/PictoHabla/Backups`** | `root` | `0:0` | `700` (`rwx------`) | POSIX (`acltype=posix`) | Eliminar ACLs heredadas que permitan lectura a usuarios no privilegiados: `setfacl -b /mnt/Seguridad/PictoHabla/Backups && chown 0:0 /mnt/Seguridad/PictoHabla/Backups && chmod 700 /mnt/Seguridad/PictoHabla/Backups`. |

El script de auditoría `precheck-truenas.sh` inspecciona los datasets y reporta discrepancias de ACL de forma **estrictamente de sólo lectura**, sin ejecutar cambios automáticos.

---

## 🛡️ 4. Validación Estricta de IP y Proxies de Confianza (Anti-Spoofing)

### Corrección Técnica: `ForwardedHeaderFilter` vs Validación de Socket TCP
En entornos con proxy inverso (como Cloudflare Tunnel), delegar la extracción de IP a filtros automáticos como `ForwardedHeaderFilter` o `RemoteIpValve` es un vector de riesgo. **`ForwardedHeaderFilter` no valida por sí mismo la identidad ni la pertenencia del peer TCP a una IP de confianza.** Si se activa indiscriminadamente o se confía en cabeceras ciegas, un cliente en la red compartida o un atacante directo podría falsificar `CF-Connecting-IP` o `X-Forwarded-For`, eludiendo las cuotas de seguridad del Rate Limiter o suplantando la identidad de otros clientes.

### Arquitectura de Seguridad Implementada:
1. **Desactivación de Interpretación Automática de Cabeceras:**  
   En `application.yml` y `application-prod.yml` se fija expresamente:
   ```yaml
   server:
     forward-headers-strategy: none
   ```
   Esto garantiza que el contenedor y Spring Boot **NUNCA reescriben `request.getRemoteAddr()`** a partir de cabeceras HTTP entrantes.
2. **Obtención del Peer TCP Físico Directo:**  
   `RateLimiterService` obtiene `remoteAddr = request.getRemoteAddr()`, que corresponde de forma inalterada a la dirección del socket TCP físico que abrió la conexión contra el contenedor.
3. **Validación Estricta de IP Específica de Proxy (`TRUSTED_PROXY_IP`):**  
   En lugar de confiar en una subred completa donde otros contenedores pudieran coexistir, se valida la IP exacta del contenedor Cloudflared (`TRUSTED_PROXY_IP=172.16.1.2`):
   ```java
   private boolean isTrustedProxy(String remoteAddr) {
       return StringUtils.hasText(trustedProxyIp) && remoteAddr.equals(trustedProxyIp);
   }
   ```
   - **Sin confianza en loopback por defecto en producción:** En producción no se presupone loopback como proxy de confianza a menos que se configure explícitamente.
   - **Sin IPs hardcodeadas en código Java:** La IP de confianza se inyecta dinámicamente mediante `@Value("${app.security.trusted-proxy-ip:}")`.
4. **Tratamiento Exclusivo de Cabeceras:**
   - **Si el peer TCP coincide exactamente con `TRUSTED_PROXY_IP`:** Se extrae `CF-Connecting-IP`, pasando por validación sintáctica estricta de formato IPv4/IPv6 (`isValidIp`). Si es válida, se utiliza como IP del cliente; si es malformada o está vacía, se realiza fallback seguro a `remoteAddr`.
   - **Descarte de `X-Forwarded-For`:** No se utiliza `X-Forwarded-For` como respaldo, eliminando el riesgo de manipulación de cadenas de proxies múltiples.
   - **Si el peer TCP NO coincide con `TRUSTED_PROXY_IP`:** **Se descarta y se ignora por completo cualquier cabecera `CF-Connecting-IP` enviada en la petición**, utilizándose indefectiblemente el peer TCP físico (`remoteAddr`). De este modo, un atacante o cliente directo queda siempre rate-limitado por su IP física real sin posibilidad de spoofing.

---

## 🩺 5. Actuator Exclusivamente Local en Puerto 8081

Para garantizar que los datos de diagnóstico del sistema nunca se expongan a la red externa ni a la red compartida de Cloudflare Tunnel:

### Configuración Aplicada en `application-prod.yml`:
```yaml
management:
  server:
    port: 8081
    address: 127.0.0.1
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
      probes:
        enabled: true
```

### Propiedades de Aislamiento Verificadas:
1. **Enlace a Loopback Local (`127.0.0.1:8081`):**  
   Al configurar `management.server.address: 127.0.0.1` y `port: 8081`, Spring Boot levanta un servidor web interno independiente en el puerto 8081 enlazado **únicamente a la interfaz loopback** dentro del namespace de red del contenedor.
2. **Inaccesible desde la Red Compartida:**  
   Otros contenedores en `ix-cloudflared_default` (incluido Cloudflared) y en `pictohabla-internal` no pueden conectar al puerto 8081, ya que las peticiones entre contenedores llegan por las interfaces virtuales `eth0`/`eth1`, donde el puerto 8081 no escucha.
3. **Inexistencia en el Puerto Público (8080):**  
   Al separar el puerto de gestión, los endpoints `/actuator/**` **dejan de registrarse en el puerto 8080**, devolviendo HTTP 404 (Not Found).
4. **Healthcheck Interno de Docker:**  
   En `source/backend/compose.truenas.yaml`:
   ```yaml
   healthcheck:
     test: ["CMD-SHELL", "curl -f -s http://127.0.0.1:8081/actuator/health > /dev/null || exit 1"]
     interval: 15s
     timeout: 5s
     retries: 3
     start_period: 45s
   ```
   El healthcheck se ejecuta localmente dentro del contenedor a través de `127.0.0.1:8081` y valida la disponibilidad real sin exponer métricas.

---

## 👥 6. Política de Registro y Recuperación de Profesores

### 1. Control Estricto de Registro de Profesores (`TEACHER_REGISTRATION_ENABLED`):
- **Deshabilitado por Defecto en Producción:** En `application-prod.yml` y `compose.truenas.yaml`, el registro público de nuevos profesores está deshabilitado por defecto (`TEACHER_REGISTRATION_ENABLED=false`).
- **Bloqueo HTTP 403 Forbidden:** Cualquier petición dirigida a `POST /api/v1/auth/register` mientras el registro esté inactivo es rechazada de inmediato con HTTP **403 Forbidden**:
  ```json
  {
    "status": "error",
    "message": "El registro de nuevos profesores está deshabilitado en este servidor. Contacte con el administrador."
  }
  ```
- **Procedimiento de Alta Segura:** Para la incorporación inicial de docentes al centro educativo:
  1. El administrador accede por SSH a `/mnt/Datos/Aplicaciones/PictoHabla/`.
  2. Activa temporalmente el registro en `.env`: `TEACHER_REGISTRATION_ENABLED=true`.
  3. Aplica los cambios: `docker compose up -d backend`.
  4. Los profesores autorizados completan su registro (protegidos por cuota estricta de 10 altas/hora por IP).
  5. El administrador restablece `TEACHER_REGISTRATION_ENABLED=false` en `.env` y ejecuta `docker compose up -d backend` para sellar definitivamente el registro público.

### 2. Comportamiento cuando SMTP no está configurado (`EMAIL_VERIFICATION_REQUIRED=false`):
- **Cuentas Activas Inmediatas:** Si el registro está habilitado y SMTP no está disponible, las cuentas se crean directamente con `emailVerified = true` y estado `ACTIVE`.
- **Recuperación de Contraseñas (`/api/v1/auth/forgot-password`):**
  - **Protección contra Falsas Confirmaciones:** Cuando el servidor SMTP no está configurado (`emailService.isAvailable() == false`), el backend **NO genera tokens huérfanos en la base de datos** ni devuelve un mensaje simulando que el correo fue enviado.
  - En su lugar, responde de forma transparente y segura con HTTP **503 Service Unavailable**:
    ```json
    {
      "status": "error",
      "message": "El servicio de recuperación de contraseñas por correo electrónico no está configurado en este servidor. Contacte con el administrador del centro."
    }
    ```
  - **Seguridad en Logs:** El sistema emite un aviso estructurado en el log del servidor (`log.warn`) indicando que el servicio SMTP está ausente. **Bajo ninguna circunstancia se registran contraseñas, hashes ni enlaces de recuperación en los logs de producción.**

---

## 📦 7. Construcción Completa de la Imagen Docker en Entorno Aislado

El directorio `/mnt/Datos/Aplicaciones/PictoHabla/source/backend` contiene todos los archivos necesarios para una compilación reproducible, determinista y autónoma con Docker multi-stage:

### Archivos Requeridos en `/mnt/Datos/Aplicaciones/PictoHabla/source/backend`:
- `Dockerfile` (Multi-stage con usuario no-root `spring:1000`, `exec java` en ENTRYPOINT).
- `compose.truenas.yaml` (Archivo canónico de orquestación para TrueNAS SCALE).
- `.dockerignore` (Exclusión estricta de `.git/`, `.gradle/`, `build/`, `.env*`, claves, certificados y volcados).
- `build.gradle.kts` (Tarea `jar` desactivada para evitar JAR plain no ejecutable; `bootJar` genera determinísticamente `pictohabla-backend.jar`).
- `settings.gradle.kts` (Nombre del proyecto y resolución de repositorios).
- `gradlew` (Script wrapper ejecutable de Gradle con permisos `chmod +x`).
- `gradle/wrapper/gradle-wrapper.jar` (Binario de arranque del wrapper).
- `gradle/wrapper/gradle-wrapper.properties` (Distribución de Gradle 8.12).
- `src/` (Árbol completo de código fuente y recursos: `src/main/java`, `src/main/resources`).

### Verificación de Compilación Aislada:
```bash
cd /mnt/Datos/Aplicaciones/PictoHabla/source/backend
docker build -t pictohabla-backend:1.0.2 .
```
La compilación multi-stage descarga Eclipse Temurin OpenJDK 21 Alpine, ejecuta `./gradlew bootJar --no-daemon` produciendo exclusivamente `pictohabla-backend.jar` y empaqueta el artefacto en una imagen mínima Alpine con usuario `spring:1000`.

---

## ⚙️ 8. Variables de Entorno y Vinculación en Compose

### Auditoría de Variables en `source/backend/compose.truenas.yaml`:
El comando `docker compose -f source/backend/compose.truenas.yaml --env-file .env` utiliza `.env` para la interpolación de variables. Para garantizar que **todas las variables lleguen al entorno del contenedor sin valores por defecto inseguros**, `compose.truenas.yaml` las declara explícitamente bajo la clave `environment:` con comprobaciones de obligatoriedad (`?Error: ...`):

| Variable `.env` | Servicio Destino | Propiedad Spring Boot Vinculada | Valor por Defecto / Validación |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `backend` | `spring.profiles.active` | `prod` |
| `DB_NAME` | `postgres`, `backend` | `POSTGRES_DB`, `spring.datasource.url` | `pictohabla` |
| `DB_USER` | `postgres`, `backend` | `POSTGRES_USER`, `spring.datasource.username` | `pictohabla_app` |
| `DB_PASSWORD` | `postgres`, `backend` | `POSTGRES_PASSWORD`, `spring.datasource.password` | Obligatoria (`:?Error: DB_PASSWORD es obligatoria`) |
| `JWT_SECRET` | `backend` | `app.jwt.secret` | Obligatoria (`:?Error: JWT_SECRET es obligatoria`) |
| `JWT_ACCESS_EXPIRATION_MINUTES` | `backend` | `app.jwt.access-token-expiration-minutes` | `15` |
| `JWT_REFRESH_EXPIRATION_DAYS` | `backend` | `app.jwt.refresh-token-expiration-days` | `7` |
| `CORS_ALLOWED_ORIGINS` | `backend` | `app.cors.allowed-origins` | Obligatoria (`:?Error: CORS_ALLOWED_ORIGINS es obligatoria`) |
| `PUBLIC_API_URL` | `backend` | `app.mail.base-url` | Obligatoria (`:?Error: PUBLIC_API_URL es obligatoria`) |
| `TRUSTED_PROXY_IP` | `backend` | `app.security.trusted-proxy-ip` | Obligatoria (`:?Error: TRUSTED_PROXY_IP es obligatoria`, ej. `172.16.1.2`) |
| `TEACHER_REGISTRATION_ENABLED` | `backend` | `app.security.teacher-registration-enabled` | `false` (`${TEACHER_REGISTRATION_ENABLED:-false}`) |
| `EMAIL_VERIFICATION_REQUIRED` | `backend` | `app.security.email-verification-required` | `false` |
| `SMTP_HOST` | `backend` | `spring.mail.host` | Vía vacía (inactiva) |
| `SMTP_PORT` | `backend` | `spring.mail.port` | `587` |
| `SMTP_USERNAME` | `backend` | `spring.mail.username` | Vía vacía |
| `SMTP_PASSWORD` | `backend` | `spring.mail.password` | Vía vacía |
| `SMTP_FROM` | `backend` | `app.mail.from` | Vía vacía (sin dominios ficticios) |
| `JAVA_OPTS` | `backend` | Opciones de máquina virtual JVM | `-XX:MaxRAMPercentage=75.0` |

---

## 🎯 9. Método de Gestión: TrueNAS Apps UI (Custom App con YAML Include) y Coherencia de Proyecto

### Mecanismo de Despliegue en TrueNAS SCALE 25.10.7:
La instalación en producción utiliza una **Custom App** gestionada desde la interfaz web de TrueNAS mediante el archivo de definición:
`/mnt/Datos/Aplicaciones/PictoHabla/custom-app.yaml`

El cual incluye el archivo canónico de Compose ubicado en el repositorio:
```yaml
include:
  - /mnt/Datos/Aplicaciones/PictoHabla/source/backend/compose.truenas.yaml
```

### Coherencia Crítica del Nombre de Proyecto Docker (`-p pictohabla`):
En TrueNAS SCALE, cuando una aplicación se instala desde la interfaz con el nombre `pictohabla`, el motor de Docker Compose subyacente asigna como nombre de proyecto `pictohabla` (o `ix-pictohabla`).
> ⚠️ **ADVERTENCIA DE PROYECTO DIVERGENTE:** Si se ejecuta `docker compose -f source/backend/compose.truenas.yaml up` sin especificar el flag de proyecto (`-p`), Docker Compose adoptará automáticamente el nombre de la carpeta contenedora (`backend`), intentando crear una segunda pila de contenedores, redes duplicadas y colisionando con los recursos existentes.

### Decisión de Arquitectura y Procedimiento Preferido:
1. **Procedimiento Preferido (Oficial):** Actualizar la **Custom App existente desde la interfaz web de TrueNAS SCALE**, preservando el archivo `custom-app.yaml` con su `include` intacto. Esto delega en el middleware de TrueNAS el ciclo de vida, la gestión de estado y la pertenencia de red sin riesgo de orfandad.
2. **Uso de CLI (Diagnóstico y Operación Avanzada):** Si se opera vía terminal SSH, **es obligatorio especificar el nombre de proyecto `pictohabla`**:
   ```bash
   cd /mnt/Datos/Aplicaciones/PictoHabla
   docker compose -p pictohabla -f source/backend/compose.truenas.yaml --env-file .env up -d backend
   ```

---

## 🧪 10. Pruebas de Conectividad con Contenedor Efímero de Diagnóstico

Debido a que el contenedor `ix-cloudflared-cloudflared-1` se distribuye sin shell ni `curl` (`scratch`), la comprobación de conectividad inter-contenedor se realiza con un contenedor efímero:

```bash
docker run --rm --network ix-cloudflared_default curlimages/curl:latest curl -s -I http://pictohabla-backend:8080/api/v1/auth/login
```

#### Salida Esperada:
```http
HTTP/1.1 405 Method Not Allowed
...
```
*(Confirma que `pictohabla-backend` resuelve en DNS interno por la red de Cloudflare y que el puerto 8080 responde peticiones HTTP).*

Comprobar que Actuator **NO es accesible** por el puerto público desde la red compartida:
```bash
docker run --rm --network ix-cloudflared_default curlimages/curl:latest curl -s -I http://pictohabla-backend:8080/actuator/health
```
#### Salida Esperada:
```http
HTTP/1.1 404 Not Found
```

---

## 👑 11. Sistema de Roles, Migraciones Flyway V2/V3 y Bootstrap Seguro del Primer Administrador

### 1. Modelo de Roles (`Role.java`):
El sistema distingue entre dos roles principales:
- `TEACHER`: Profesor con acceso exclusivo a sus propias aulas y alumnos (Aislamiento multitenant). No puede acceder a operaciones administrativas.
- `ADMIN`: Administrador del centro educativo con autoridad global para futuras operaciones de gestión. No participa en la docencia de aulas ni acceso a estudiantes (`@PreAuthorize("hasRole('TEACHER')")`).

### 2. Migraciones Flyway V2 y V3:

#### `V2__add_teacher_role.sql`:
```sql
ALTER TABLE teachers ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'TEACHER';
ALTER TABLE teachers ADD CONSTRAINT chk_teachers_role CHECK (role IN ('ADMIN', 'TEACHER'));
CREATE INDEX idx_teachers_role ON teachers(role);
```
- **Conservación de Datos:** Todos los profesores existentes en PostgreSQL se convierten automáticamente en `TEACHER`.
- **Integridad:** La restricción `chk_teachers_role` impide registrar roles no contemplados.
- **Inmutabilidad:** Ni contraseñas ni identificadores existentes son modificados.

#### `V3__admin_bootstrap_protection.sql`:
```sql
-- Tabla persistente para registrar de forma indeleble que el bootstrap ya se ejecutó
CREATE TABLE IF NOT EXISTS admin_bootstrap_metadata (
    id INT DEFAULT 1 PRIMARY KEY,
    completed BOOLEAN NOT NULL DEFAULT TRUE,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    admin_email VARCHAR(255) NOT NULL,
    CONSTRAINT chk_single_bootstrap_record CHECK (id = 1)
);

-- Columna generada y restricción única para garantizar como máximo 1 administrador en teachers
-- En PostgreSQL 16 es OBLIGATORIO indicar explícitamente STORED
ALTER TABLE teachers ADD COLUMN admin_singleton VARCHAR(10) GENERATED ALWAYS AS (
    CASE WHEN role = 'ADMIN' THEN 'ADMIN' ELSE NULL END
) STORED;

ALTER TABLE teachers ADD CONSTRAINT uq_teachers_single_admin UNIQUE (admin_singleton);
```
- **Protección a Nivel de Base de Datos:** Si se intenta insertar un segundo registro con `role = 'ADMIN'`, la columna generada evalúa a `'ADMIN'`, disparando de inmediato una violación de la restricción `uq_teachers_single_admin`. Para los profesores (`role = 'TEACHER'`), la columna evalúa a `NULL`, lo cual permite ilimitados registros de profesores bajo la especificación SQL de unicidad.
- **Marca Persistente Indeleble:** La tabla `admin_bootstrap_metadata` tiene `id = 1 PRIMARY KEY` y un `CHECK (id = 1)`, garantizando que solo puede existir un registro histórico. Si posteriormente se eliminase la cuenta del administrador, el bootstrap sigue bloqueado permanentemente gracias a este registro.

### 3. Garantías de Concurrencia Multi-Proceso y Seguridad:
1. **Bloqueo Transaccional Consultivo de PostgreSQL (`pg_advisory_xact_lock`):**  
   Antes de evaluar la existencia de un administrador, el servicio adquiere el bloqueo transaccional `SELECT pg_advisory_xact_lock(7821948201948201)` en PostgreSQL. El bloqueo se adquiere sobre la misma conexión física del DataSource vinculada a la transacción de `TransactionTemplate`. Se mantiene durante toda la transacción hasta el `COMMIT`/`ROLLBACK`, serializando cualquier intento concurrente proveniente de múltiples contenedores o procesos Java independientes.
2. **Atomicidad de Inserción y Metadatos:**  
   La persistencia de la cuenta `Teacher` y la inserción del registro en `admin_bootstrap_metadata` ocurren dentro de la **misma transacción relacional**. Si alguna de las dos falla, la transacción revierte por completo (rollback) y no queda ningún dato huérfano.
3. **Monitor Sincronizado en JVM:**  
   Un bloque `synchronized (JVM_LOCK)` envuelve el `TransactionTemplate`, serializando también los hilos dentro del mismo proceso.
4. **Cifrado BCrypt Coste 12:**  
   La contraseña del administrador se procesa exclusivamente con BCrypt coste 12.
5. **Sin Endpoints HTTP:**  
   El runner de bootstrap solo se activa si se especifica la propiedad `--pictohabla.bootstrap.admin=true`. No registra controladores REST ni expone ningún puerto.

### 4. Lanzamiento Docker del Bootstrap (Uso de `--entrypoint java` y Flujo por stdin):

> ⚠️ **IMPORTANTE: Pérdida de Parámetros con el ENTRYPOINT por Defecto:**  
> La imagen de producción tiene `ENTRYPOINT ["sh", "-c", "exec java ..."]`. Cuando se invoca `docker run ... imagen param1 param2`, la shell `sh -c` descarta los parámetros posicionales adicionales, impidiendo que lleguen a la JVM.  
> Por ello, el comando de bootstrap **debe sobreescribir el punto de entrada con `--entrypoint java`**, garantizando que `-Dspring.main.web-application-type=none` y los argumentos de Spring Boot se entreguen directamente al proceso Java.

> 🔒 **Seguridad de la Contraseña (Anti-Inspect / Anti-PS):**  
> Para evitar que la contraseña quede expuesta en `docker inspect` o en la tabla de procesos (`ps aux`), las credenciales no se pasan como variables de entorno (`-e ADMIN_PASSWORD=...`) ni en argumentos de CLI. En su lugar, se canalizan mediante un pipe por la entrada estándar (`stdin`) directamente al proceso Java.

#### Comando Probado de Bootstrap:
```bash
printf '%s\n%s\n%s\n' "admin@colegio.es" "Administrador Principal" "MiPasswordSeguro2026" | \
docker run --rm -i \
    --network ix-pictohabla_pictohabla-internal \
    --env-file /mnt/Datos/Aplicaciones/PictoHabla/.env \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e DB_HOST=postgres \
    -e DB_PORT=5432 \
    --entrypoint java \
    pictohabla-backend:1.0.2 \
    -Dspring.main.web-application-type=none \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -jar app.jar \
    --pictohabla.bootstrap.admin=true
```

#### Ejecución Asistida mediante Script (`bootstrap-admin.sh`):
Para evitar exponer contraseñas en el historial de bash, se utiliza el script interactivo:
```bash
cd /mnt/Datos/Aplicaciones/PictoHabla
bash scripts/bootstrap-admin.sh
```
El script solicita el correo, nombre y contraseña con `read -s` (oculta, con verificación), verifica que la red `ix-pictohabla_pictohabla-internal` existe (deteniéndose con error crítico si no se encuentra), entrega los datos por stdin al contenedor efímero y limpia las variables de memoria al finalizar (`unset`).

---

## 🔑 11b. Procedimiento Local de Recuperación de Contraseña del Administrador

Si el administrador olvida su contraseña y el servicio SMTP no está configurado todavía, **NUNCA se debe eliminar la tabla `admin_bootstrap_metadata` ni intentar crear otra cuenta ADMIN** (lo cual sería rechazado por la base de datos).

Para restablecer la contraseña de forma segura en local:

1. **Conectar a la consola de PostgreSQL en TrueNAS SCALE:**
   ```bash
   docker exec -it pictohabla-postgres psql -U pictohabla_app -d pictohabla
   ```
2. **Generar un nuevo hash BCrypt (coste 12):**  
   Puede generarse con Python en el host o mediante un contenedor efímero:
   ```bash
   python3 -c "import bcrypt; print(bcrypt.hashpw(b'TuNuevaContrasenaSegura2026', bcrypt.gensalt(12)).decode())"
   ```
   *(Ejemplo de hash resultante: `$2a$12$e8YqJq0xZ...`)*.
3. **Actualizar el registro del administrador en la base de datos:**
   ```sql
   UPDATE teachers 
   SET password_hash = '$2a$12$NUEVO_HASH_BCRYPT_AQUI',
       failed_login_attempts = 0,
       locked_until = NULL,
       updated_at = NOW()
   WHERE email = 'admin@colegio.es' AND role = 'ADMIN';
   ```
4. **Verificar la actualización:**
   ```sql
   SELECT id, email, role, status, failed_login_attempts, locked_until FROM teachers WHERE role = 'ADMIN';
   ```
Este procedimiento actualiza exclusivamente la clave, desbloquea la cuenta si fue bloqueada por fuerza bruta, mantiene inalterados los roles y conserva intacta la marca indeleble de bootstrap en `admin_bootstrap_metadata`.

---

## 🔄 12. Procedimiento de Actualización a la Versión 1.0.2 en TrueNAS SCALE

Para actualizar el backend desde la versión 1.0.1 a la **1.0.2** (corrección de compatibilidad con PostgreSQL 16 en el bootstrap del administrador) sin pérdida de datos, sin tocar PostgreSQL y sin interrumpir las demás aplicaciones:

### Opción A: Actualización vía TrueNAS SCALE Apps UI (Procedimiento Preferido)
1. **Actualizar el código y compilar la nueva imagen en el host (SSH):**
   ```bash
   cd /mnt/Datos/Aplicaciones/PictoHabla/source/backend
   git pull origin main
   docker build -t pictohabla-backend:1.0.2 .
   ```
2. **Ejecutar auditoría de prechequeo de sólo lectura:**
   ```bash
   cd /mnt/Datos/Aplicaciones/PictoHabla
   bash scripts/precheck-truenas.sh
   ```
3. **Actualizar la Custom App en la interfaz web de TrueNAS SCALE:**
   - Navegar a **Apps** → pestaña **Installed Apps**.
   - Localizar la aplicación `pictohabla` y hacer clic en **Edit**.
   - Comprobar que mantiene la referencia a `/mnt/Datos/Aplicaciones/PictoHabla/custom-app.yaml` (o `source/backend/compose.truenas.yaml` con imagen `pictohabla-backend:1.0.2`).
   - Hacer clic en **Save** / **Update**.
   - TrueNAS recreará automáticamente el contenedor del backend con la imagen 1.0.2 en la red `ix-pictohabla_pictohabla-internal`, manteniendo PostgreSQL intacto.
4. **Verificar los logs de arranque y migraciones:**
   - En la tarjeta de la app `pictohabla`, inspeccionar los logs del backend para confirmar que el backend 1.0.2 inició correctamente.
5. **Ejecutar el bootstrap para crear el primer administrador:**
   ```bash
   cd /mnt/Datos/Aplicaciones/PictoHabla
   bash scripts/bootstrap-admin.sh
   ```

### Opción B: Actualización vía Docker Compose CLI (Avanzado)
Si se gestiona directamente desde la consola SSH, **es obligatorio especificar el nombre de proyecto `pictohabla`** para no crear una pila aislada paralela:
```bash
# 1. Acceder al repositorio del backend y compilar
cd /mnt/Datos/Aplicaciones/PictoHabla/source/backend
git pull origin main
docker build -t pictohabla-backend:1.0.2 .

# 2. Volver al directorio raíz y verificar
cd /mnt/Datos/Aplicaciones/PictoHabla
bash scripts/precheck-truenas.sh

# 3. Aplicar actualización sobre el proyecto existente pictohabla
docker compose -p pictohabla -f source/backend/compose.truenas.yaml --env-file .env up -d backend

# 4. Observar logs de Flyway
docker compose -p pictohabla -f source/backend/compose.truenas.yaml logs -f backend

# 5. Ejecutar bootstrap del administrador
bash scripts/bootstrap-admin.sh
```

---

## 📊 13. Batería de Pruebas Automatizadas

El backend dispone de **74 pruebas unitarias y de integración** automatizadas, todas ellas en estado satisfactorio (0 fallos, 0 errores, 0 omitidas):

```
BUILD SUCCESSFUL in 27s
74 tests completed, 0 failed, 0 skipped
```

### Desglose Detallado por Suite de Pruebas:

- **`AdminBootstrapServiceTest` (8 pruebas):**
  - Creación exitosa del primer administrador cuando no existe ninguno (`Role.ADMIN`, `ACTIVE`, `emailVerified=true`).
  - Rechazo de segundo intento de bootstrap con `IllegalStateException` cuando ya existe un administrador.
  - Ejecuciones concurrentes mediante hilos simultáneos: exactamente una tiene éxito y se crea exactamente un administrador.
  - Rechazo de datos de entrada inválidos (correo malformado, contraseña débil, nombre corto).
  - Restricción relacional en base de datos: `uq_teachers_single_admin` impide registrar un segundo `Role.ADMIN` (`DataIntegrityViolationException`).
  - Restricción relacional en base de datos: Múltiples registros con `Role.TEACHER` permitidos sin colisión de unicidad.
  - Restricción relacional en `admin_bootstrap_metadata`: Impide más de un registro de bootstrap (`id = 1 PRIMARY KEY` y `CHECK (id = 1)`).
  - Persistencia indeleble: El bootstrap permanece bloqueado permanentemente aunque la cuenta `ADMIN` sea borrada de `teachers`.

- **`TeacherRoleSecurityTest` (13 pruebas):**
  - Un nuevo profesor registrado recibe inmutablemente el rol `TEACHER`.
  - Intento de inyección de `role=ADMIN` en el payload de registro es ignorado y se asigna `TEACHER`.
  - Un profesor no puede modificar su rol mediante actualización de perfil (`/api/v1/teachers/me`).
  - Un usuario con rol `TEACHER` no puede acceder a operaciones administrativas (rechazado con `AccessDeniedException`).
  - Un administrador dispone de la autoridad `ROLE_ADMIN` y accede a operaciones administrativas.
  - Los profesores mantienen el acceso a sus propias aulas.
  - Un profesor no puede acceder a las aulas de otro profesor (`ResourceNotFoundException`).
  - El inicio de sesión y la rotación de tokens (*refresh*) preservan el rol de la cuenta (`TEACHER` o `ADMIN`).
  - Los tokens JWT contienen el claim `role` correcto.
  - El componente de bootstrap no abre ningún endpoint HTTP (no es Controller ni RestController).
  - Un usuario con rol `ADMIN` tiene bloqueado el acceso a endpoints de aulas y alumnos (HTTP 403 Forbidden).
  - `ClassroomService` rechaza la creación de aulas a administradores con `ForbiddenException`.
  - Token JWT forjado con `role=ADMIN` para un usuario docente es detectado y rechazado por `JwtAuthenticationFilter` (HTTP 401).

- **`FlywayMigrationV2Test` (1 prueba):**
  - La migración V1 → V2 conserva íntegramente los usuarios existentes y les asigna el rol `TEACHER` por defecto.
  - La restricción CHECK a nivel de base de datos impide registrar roles inválidos.

- **`RateLimiterServiceTest` (14 pruebas):**
  - Validación de peer TCP contra `TRUSTED_PROXY_IP=172.16.1.2`, descarte de `X-Forwarded-For`, no loopback en producción, cuotas y anti-fuerza bruta.

- **`ActuatorSecurityTest` (3 pruebas):**
  - Aislamiento en loopback 8081, endpoints no expuestos en puerto público 8080 (404).

- **`AuthServiceTest` (12 pruebas):**
  - Flujo de registro, login, refresh, bloqueo por fallos, rechazo 403 cuando registro está deshabilitado y rechazo 503 sin SMTP.

- **`AuthControllerTest` (9 pruebas):**
  - Endpoints REST de autenticación, validación de cuotas y rechazo 403 cuando el registro está cerrado.

- **`ClassroomControllerTest` (5 pruebas):**
  - CRUD de aulas con aislamiento multitenant por profesor.

- **`JwtServiceTest` (4 pruebas):**
  - Generación y validación de tokens con roles `TEACHER` y `ADMIN`, extracción de claims y firma HMAC-SHA256.

- **`StudentControllerTest` (2 pruebas):**
  - Pseudónimos y privacidad infantil por diseño.

- **`TeacherControllerTest` (2 pruebas):**
  - Consulta y actualización de perfil del profesor autenticado.

- **`BackendApplicationTests` (1 prueba):**
  - Inicialización completa del contexto Spring Boot y aplicación de migraciones Flyway V1, V2 y V3.
  - Inicialización completa del contexto Spring Boot y aplicación de migraciones Flyway V1 y V2.
