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

Toda la operativa de PictoHabla se centraliza bajo una estructura canónica limpia y unificada en el pool `Datos`, separando el almacenamiento de copias en el pool `Seguridad`:

```
/mnt/Datos/PictoHabla/
├── compose.yaml                  # Archivo de orquestación de Docker Compose
├── .env                          # Variables de entorno de producción (chmod 600)
├── scripts/                      # Scripts de mantenimiento y auditoría (chmod 700 / 755)
│   ├── backup.sh                 # Backup diario automático (PostgreSQL custom dump)
│   ├── restore.sh                # Restauración asistida con confirmación
│   └── precheck-truenas.sh       # Auditoría de sólo lectura previa al despliegue
├── build/                        # [Sólo si se compila en el NAS] Contexto completo de compilación
│   ├── Dockerfile                # Multi-stage Dockerfile no-root
│   ├── build.gradle.kts          # Definición de dependencias y plugins
│   ├── settings.gradle.kts       # Configuración de repositorios y módulos
│   ├── gradlew                   # Script wrapper de Gradle (chmod +x)
│   ├── gradle/
│   │   └── wrapper/
│   │       ├── gradle-wrapper.jar
│   │       └── gradle-wrapper.properties
│   └── src/                      # Código fuente y recursos de backend/
├── Postgres/                     # Dataset ZFS Datos/PictoHabla/Postgres (UID 70:70, perm 700)
└── Uploads/                      # Dataset ZFS Datos/PictoHabla/Uploads (UID 1000:1000, perm 750)

/mnt/Seguridad/PictoHabla/
└── Backups/                      # Dataset ZFS Seguridad/PictoHabla/Backups (root:root, perm 700)
```

### Ubicación de Componentes Clave:
| Elemento | Ruta en TrueNAS SCALE | Permisos Recomendados |
| :--- | :--- | :--- |
| **Directorio de Trabajo** | `/mnt/Datos/PictoHabla` | `755` (`root:root`) |
| **Orquestación Compose** | `/mnt/Datos/PictoHabla/compose.yaml` | `644` (`root:root`) |
| **Configuración Secreta** | `/mnt/Datos/PictoHabla/.env` | `600` (`root:root`) |
| **Scripts de Operación** | `/mnt/Datos/PictoHabla/scripts/*.sh` | `700` (`root:root`) |
| **Dataset BD Postgres** | `/mnt/Datos/PictoHabla/Postgres` | `700` (`70:70` - `postgres:postgres`) |
| **Dataset Subidas Multimedia** | `/mnt/Datos/PictoHabla/Uploads` | `750` (`1000:1000` - `spring:spring`) |
| **Dataset Copias Seguridad** | `/mnt/Seguridad/PictoHabla/Backups` | `700` (`0:0` - `root:root`) |
| **Imagen Docker generada** | Daemon local de Docker (`pictohabla-backend:1.0.0`) | N/A (Docker Image Store) |

---

## 🔒 3. Auditoría de ACLs y Permisos POSIX en TrueNAS SCALE

> ⚠️ **ADVERTENCIA CRÍTICA DE ACL EN TRUENAS:** Antes de aplicar cambios de UID, GID o permisos POSIX (`chown`/`chmod`), es **obligatorio verificar el tipo de ACL** de cada dataset. TrueNAS SCALE soporta ACLs avanzadas tipo NFSv4/SMB. Si un dataset se creó con un perfil SMB o ACLs NFSv4 activas, los comandos POSIX estándar fallarán o serán sobreescritos por las ACEs (Access Control Entries) heredadas, impidiendo que PostgreSQL o Spring Boot inicien.

### Tabla de Auditoría de Datasets y ACLs:

| Dataset | Usuario esperado | UID:GID | Permisos necesarios | Tipo ACL a comprobar | Qué hacer si existe una ACL incompatible |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`Datos/PictoHabla/Postgres`** | `postgres` | `70:70` | `700` (`rwx------`) | POSIX (`acltype=posix`, `aclmode=discard`) | Si tiene ACL NFSv4 o entradas ACE heredadas, en la UI de TrueNAS ir a **Datasets** -> seleccionar `Postgres` -> **Edit Permissions** -> seleccionar perfil **POSIX Standard** y marcar **Strip ACLs**. Vía CLI: `setfacl -b /mnt/Datos/PictoHabla/Postgres && chown 70:70 /mnt/Datos/PictoHabla/Postgres && chmod 700 /mnt/Datos/PictoHabla/Postgres`. |
| **`Datos/PictoHabla/Uploads`** | `spring` | `1000:1000` | `750` (`rwxr-x---`) | POSIX (`acltype=posix`, `aclmode=discard`) | Si una ACL restrictiva bloquea a UID 1000, cambiar tipo a POSIX o añadir entrada ACE para UID 1000 (`rwx`). Vía CLI: `setfacl -b /mnt/Datos/PictoHabla/Uploads && chown 1000:1000 /mnt/Datos/PictoHabla/Uploads && chmod 750 /mnt/Datos/PictoHabla/Uploads`. |
| **`Seguridad/PictoHabla/Backups`** | `root` | `0:0` | `700` (`rwx------`) | POSIX (`acltype=posix`) | Eliminar ACLs heredadas que permitan lectura a usuarios no privilegiados: `setfacl -b /mnt/Seguridad/PictoHabla/Backups && chown 0:0 /mnt/Seguridad/PictoHabla/Backups && chmod 700 /mnt/Seguridad/PictoHabla/Backups`. |

El script de auditoría `precheck-truenas.sh` inspecciona los datasets y reporta discrepancias de ACL de forma **estrictamente de sólo lectura**, sin ejecutar cambios automáticos.

---

## 🛡️ 4. Validación Estricta de IP y Proxies de Confianza (Anti-Spoofing)

### Corrección Técnica: `ForwardedHeaderFilter` vs Validación de Socket TCP
En entornos con proxy inverso (como Cloudflare Tunnel), la tentación habitual es delegar la extracción de IP a filtros automáticos como `ForwardedHeaderFilter` o `RemoteIpValve`. **Sin embargo, `ForwardedHeaderFilter` no realiza por sí mismo una validación de pertenencia a subred CIDR de la IP de origen.** Si se activa indiscriminadamente, un cliente malicioso que logre enviar una petición directa podría falsificar `X-Forwarded-For` o `CF-Connecting-IP`, eludiendo el limitador de tasa (Rate Limiter) o haciéndose pasar por otra IP.

### Arquitectura de Seguridad Implementada:
1. **Desactivación de Interpretación Automática de Cabeceras:**  
   En `application.yml` y `application-prod.yml` se fija expresamente:
   ```yaml
   server:
     forward-headers-strategy: none
   ```
   Esto garantiza que el contenedor y Spring Boot **NUNCA reescriben `request.getRemoteAddr()`** con cabeceras externas.
2. **Obtención del Peer TCP Físico:**  
   `RateLimiterService` obtiene `remoteAddr = request.getRemoteAddr()`, que corresponde de forma inalterada a la dirección del socket TCP que abrió la conexión contra el contenedor.
3. **Validación Cripto-Determinista de Subred de Confianza:**  
   Se verifica si `remoteAddr` pertenece a `127.0.0.1` / `::1` o a la subred real de Cloudflare Tunnel (`TRUSTED_PROXY_SUBNET`, ej. `172.16.1.0/24`).
4. **Tratamiento de Cabeceras HTTP:**
   - **Si el peer TCP ES de confianza:** Se extrae `CF-Connecting-IP` (o en su defecto el primer segmento de `X-Forwarded-For`). La IP extraída es sometida a validación sintáctica estricta (`isValidIp`). Si es válida, se utiliza; si es malformada o ausente, se realiza fallback seguro a `remoteAddr`.
   - **Si el peer TCP NO ES de confianza:** **Se descarta y se ignora cualquier cabecera `CF-Connecting-IP` o `X-Forwarded-For` enviada por el cliente**, utilizándose estrictamente el peer TCP físico (`remoteAddr`). De este modo, un atacante directo queda invariablemente rate-limitado por su IP física real.

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
   En `compose.yaml`:
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

### Comportamiento cuando SMTP no está configurado (`EMAIL_VERIFICATION_REQUIRED=false`):

1. **Registro de Profesores:**
   - Las cuentas se registran con `emailVerified = true` y estado `ACTIVE`.
   - El endpoint `/api/v1/auth/register` está protegido por cuota estricta en `RateLimiterService` (máximo **10 registros por hora por IP**).
   - **Recomendación para Lanzamiento:** Para un entorno escolar controlado sin servidor SMTP público, se recomienda registrar las cuentas de los profesores autorizados durante la puesta en marcha inicial y, si se requiere restringir registros futuros, limitar el acceso al endpoint o gestionar las cuentas administrativamente.

2. **Recuperación de Contraseñas (`/api/v1/auth/forgot-password`):**
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

El directorio `/mnt/Datos/PictoHabla/build` debe contener todos los archivos necesarios para una compilación reproducible y autónoma con Docker multi-stage:

### Archivos Requeridos en `/mnt/Datos/PictoHabla/build`:
- `Dockerfile` (Multi-stage con usuario no-root `spring:1000`).
- `build.gradle.kts` (Dependencias y configuración de Spring Boot 3.5.x).
- `settings.gradle.kts` (Nombre del proyecto y resolución de plugins).
- `gradlew` (Script wrapper ejecutable de Gradle con permisos `chmod +x`).
- `gradle/wrapper/gradle-wrapper.jar` (Binario de arranque del wrapper).
- `gradle/wrapper/gradle-wrapper.properties` (Distribución de Gradle 8.12).
- `src/` (Árbol completo de código fuente y recursos: `src/main/java`, `src/main/resources`).

### Verificación de Compilación Aislada:
```bash
cd /mnt/Datos/PictoHabla/build
docker build -t pictohabla-backend:1.0.0 .
```
La compilación multi-stage descarga OpenJDK 21, ejecuta `./gradlew bootJar` y empaqueta el JAR resultante en una imagen mínima Alpine con usuario `1000:1000`.

---

## ⚙️ 8. Variables de Entorno y Vinculación en Compose

### Auditoría de Variables en `compose.yaml`:
El comando `docker compose --env-file .env` utiliza `.env` para la interpolación de variables. Sin embargo, para garantizar que **todas las variables lleguen al entorno del contenedor**, `compose.yaml` las declara explícitamente bajo la clave `environment:` con comprobaciones de obligatoriedad (`?Error: ...`):

| Variable `.env` | Servicio Destino | Propiedad Spring Boot Vinculada | Valor por Defecto / Validación |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `backend` | `spring.profiles.active` | `prod` |
| `DB_NAME` | `postgres`, `backend` | `POSTGRES_DB`, `spring.datasource.url` | `pictohabla` |
| `DB_USER` | `postgres`, `backend` | `POSTGRES_USER`, `spring.datasource.username` | `pictohabla_app` |
| `DB_PASSWORD` | `postgres`, `backend` | `POSTGRES_PASSWORD`, `spring.datasource.password` | Obligatoria (`:?Error`) |
| `JWT_SECRET` | `backend` | `app.jwt.secret` | Obligatoria (`:?Error`) |
| `JWT_ACCESS_EXPIRATION_MINUTES` | `backend` | `app.jwt.access-token-expiration-minutes` | `15` |
| `JWT_REFRESH_EXPIRATION_DAYS` | `backend` | `app.jwt.refresh-token-expiration-days` | `7` |
| `CORS_ALLOWED_ORIGINS` | `backend` | `app.cors.allowed-origins` | `https://pictohabla.local` |
| `PUBLIC_API_URL` | `backend` | `app.mail.base-url` | `https://api.pictohabla.local` |
| `TRUSTED_PROXY_SUBNET` | `backend` | `app.security.trusted-proxy-subnet` | `172.16.1.0/24` |
| `EMAIL_VERIFICATION_REQUIRED` | `backend` | `app.security.email-verification-required` | `false` |
| `SMTP_HOST` | `backend` | `spring.mail.host` | Vía vacía (inactiva) |
| `SMTP_PORT` | `backend` | `spring.mail.port` | `587` |
| `SMTP_USERNAME` | `backend` | `spring.mail.username` | Vía vacía |
| `SMTP_PASSWORD` | `backend` | `spring.mail.password` | Vía vacía |
| `SMTP_FROM` | `backend` | `app.mail.from` | `no-reply@pictohabla.local` |
| `JAVA_OPTS` | `backend` | Opciones de máquina virtual JVM | `-XX:MaxRAMPercentage=75.0` |

---

## 🎯 9. Método de Gestión: Docker Compose CLI vía SSH vs TrueNAS Apps UI

### Relación entre Docker Compose CLI y la UI de TrueNAS SCALE 25.10.7:
TrueNAS SCALE 25.10.7 utiliza internamente Docker Compose como motor para sus aplicaciones. No obstante, TrueNAS mantiene una base de datos propia de middleware (`apps.sqlite`) que registra y controla el ciclo de vida de las aplicaciones creadas mediante su interfaz gráfica (**Custom App** / **Install via YAML**).

### Comparativa:

| Criterio | Docker Compose CLI vía SSH (Método Recomendado) | TrueNAS Custom App (UI) |
| :--- | :--- | :--- |
| **Control de Configuración** | **Total y determinista.** El archivo `.env` se lee directamente sin intermediarios ni transformaciones de formato. | Propenso a errores en la carga de variables multilínea o secretos con caracteres especiales. |
| **Redes Externas Preexistentes** | **Nativo y sin fricción.** Se conecta a `ix-cloudflared_default` como `external: true` de forma inmediata. | A menudo falla al intentar recrear la red o exigir dependencias no reconocidas por el catálogo. |
| **Visibilidad de Logs** | `docker compose logs -f` muestra la traza continua de Flyway y arranque en tiempo real. | Truncamiento de logs en arranques largos y paginación en el visor web. |
| **Aislamiento de Estado** | No interfiere con la base de datos interna de apps de TrueNAS ni altera otras aplicaciones (`cloudflared`, `tailscale`, etc.). | Si la UI se desincroniza, puede marcar la app como errónea e intentar reiniciarla en bucle. |

### Decisión de Arquitectura:
Se establece **Docker Compose CLI vía SSH** como el **método oficial y exclusivo**.
> 🛑 **Regla de Operación:** NUNCA intentes gestionar el mismo despliegue mezclando ambos sistemas. Si se inicia por CLI, mantén todas las operaciones (paradas, actualizaciones y copias) por CLI.

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

## 📊 11. Batería de Pruebas Automatizadas

El backend dispone de **50 pruebas unitarias y de integración** automatizadas, todas ellas en estado satisfactorio (0 fallos, 0 errores, 0 omitidas):

```
BUILD SUCCESSFUL in 22s
50 tests completed, 0 failed, 0 skipped
```

- **`RateLimiterServiceTest` (15 pruebas):**
  - Confianza en Loopback (`127.0.0.1`, `::1`).
  - Confianza en subred de Cloudflare (`172.16.1.0/24`).
  - Detección y descarte de cabecera `CF-Connecting-IP` falseada desde cliente directo.
  - Detección y descarte de cabecera `X-Forwarded-For` falseada desde cliente directo.
  - Descarte simultáneo de cabeceras falseadas.
  - Conexión desde subred Docker no confiable.
  - Manejo seguro de cabeceras ausentes (fallback a TCP peer).
  - Manejo de cabecera `CF-Connecting-IP` malformada (inyecciones SQL, caracteres inválidos).
  - Manejo de ambas cabeceras malformadas.
  - Cambio dinámico de subred (`setTrustedProxySubnet`) y actualización de confianza.
  - Validación sintáctica de IPs IPv4 e IPv6 sin resolución DNS.
  - Reglas de cuotas por acción e IP.
  - Bloqueo por fuerza bruta y restablecimiento.
- **`ActuatorSecurityTest` (3 pruebas):**
  - Actuator en puerto de gestión independiente (8081 en prod) responde `200 UP` sin detalles sensibles de base de datos ni disco.
  - Endpoints deshabilitados (`/actuator/info`, `/actuator/env`) no son accesibles públicamente.
  - El puerto principal de la API (8080) **no expone `/actuator/health`** (devuelve 404).
- **`AuthServiceTest` (11 pruebas):**
  - Registro de profesor, duplicados y flujo de verificación.
  - Inicio de sesión, contraseñas incorrectas y bloqueo de cuenta.
  - Rotación de refresh tokens y revocación por reutilización de token robado.
  - Restablecimiento de contraseña con token válido.
  - **Recuperación rechazada con HTTP 503 cuando SMTP no está configurado.**
  - **Recuperación aceptada y envío de correo cuando SMTP está disponible.**
- **`AuthControllerTest` (8 pruebas):**
  - Endpoints REST de autenticación, serialización JSON y rate-limiting.
- **`ClassroomControllerTest` (5 pruebas):**
  - Creación y listado de aulas vinculadas al profesor autenticado.
- **`JwtServiceTest` (3 pruebas):**
  - Firmas HMAC-SHA256, expiración y validación de claims.
- **`StudentControllerTest` (2 pruebas):**
  - Creación y asignación de alumnos sin cuenta de usuario.
- **`TeacherControllerTest` (2 pruebas):**
  - Perfil de profesor y protección por rol `ROLE_TEACHER`.
- **`BackendApplicationTests` (1 prueba):**
  - Inicialización completa del contexto de Spring Boot y migraciones de Flyway.
