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
├── build/                        # [Sólo si se compila en el NAS] Código fuente del backend
│   ├── Dockerfile
│   ├── build.gradle.kts
│   ├── gradlew / gradlew.bat
│   ├── gradle/
│   └── src/
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

## 👤 3. Auditoría de Usuarios, UIDs y Permisos POSIX

Para cumplir el principio de mínimo privilegio y evitar colisiones de permisos entre el host ZFS y los contenedores:

1. **PostgreSQL 16 Alpine (`postgres:16-alpine`):**
   - Se ejecuta internamente como usuario no privilegiado `postgres` con **UID `70`** y **GID `70`**.
   - Comando de configuración en el host:
     ```bash
     chown 70:70 /mnt/Datos/PictoHabla/Postgres
     chmod 700 /mnt/Datos/PictoHabla/Postgres
     ```
   - Si no se asigna UID 70, la inicialización de PostgreSQL (`initdb`) fallará con error `Permission denied` al crear el catálogo del clúster.

2. **Backend Spring Boot 3.5 (`pictohabla-backend:1.0.0`):**
   - El Dockerfile multi-stage crea un grupo y usuario de sistema: `spring:spring` con **UID `1000`** y **GID `1000`**, y cambia a `USER spring` antes del arranque.
   - El directorio `/mnt/Datos/PictoHabla/Uploads` se monta dentro del contenedor en `/app/uploads`.
   - Comando de configuración en el host:
     ```bash
     chown 1000:1000 /mnt/Datos/PictoHabla/Uploads
     chmod 750 /mnt/Datos/PictoHabla/Uploads
     ```

3. **Copias de Seguridad (`/mnt/Seguridad/PictoHabla/Backups`):**
   - Debe pertenecer exclusivamente al usuario administrador de TrueNAS:
     ```bash
     mkdir -p /mnt/Seguridad/PictoHabla/Backups
     chown 0:0 /mnt/Seguridad/PictoHabla/Backups
     chmod 700 /mnt/Seguridad/PictoHabla/Backups
     ```

4. **Archivo `.env`:**
   - Contiene credenciales maestras y el secreto JWT. Ningún usuario no-root debe poder leerlo:
     ```bash
     chmod 600 /mnt/Datos/PictoHabla/.env
     ```

> 🛑 **Regla de Oro:** NUNCA uses `chmod -R 777` en los datasets ni alteres permisos en las raíces `/mnt/Datos` o `/mnt/Seguridad`.

---

## 🛡️ 4. Seguridad y Hardening de Spring Boot Actuator

Para producción, la configuración en `application-prod.yml` ha sido endurecida para impedir la divulgación de información sensible (Information Disclosure):

### Parámetros Aplicados en Producción:
- **Exposición Web Mínima:**
  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: health    # SÓLO health. Se han eliminado info, beans, env, metrics.
    endpoint:
      health:
        show-details: never  # NUNCA revela conexiones JDBC, disco o componentes internos.
  ```
- **Respuesta Pública de Salud:**
  Devuelve exclusivamente un payload binario simple sin detalles de infraestructura:
  ```json
  {"status":"UP"}
  ```
- **Supresión de Trazas de Error:**
  `server.error.include-message: never`, `server.error.include-stacktrace: never` y `server.error.include-exception: false`.

### Monitoreo Seguro sin Exposición a Internet:
1. **Healthcheck nativo de Docker (aislado en localhost del contenedor):**
   Configurado en `compose.yaml`:
   ```yaml
   test: ["CMD-SHELL", "curl -f -s http://127.0.0.1:8080/actuator/health > /dev/null || exit 1"]
   ```
2. **Inspección desde la shell de TrueNAS:**
   ```bash
   docker inspect --format='{{json .State.Health}}' pictohabla-backend
   ```
3. **Regla de Bloqueo en Cloudflare Zero Trust:**
   En el panel de Cloudflare Zero Trust, se recomienda añadir una regla de bloqueo de ruta (WAF / HTTP Request Block) para cualquier petición externa dirigida a `/actuator/**`, garantizando que únicamente la API REST `/api/**` o las rutas públicas de autenticación sean consumidas desde el exterior.

---

## ⚙️ 5. Variables de Entorno y Fallback de SMTP

Copia la plantilla `backend/.env.truenas.example` a `/mnt/Datos/PictoHabla/.env`:

```bash
cp /mnt/Datos/PictoHabla/build/.env.truenas.example /mnt/Datos/PictoHabla/.env
chmod 600 /mnt/Datos/PictoHabla/.env
```

Genera secretos criptográficos con `openssl rand -hex 32` y reemplaza los marcadores de posición:

```env
# ==============================================================================
# PictoHabla Backend — Variables de Entorno de Producción
# ==============================================================================
SPRING_PROFILES_ACTIVE=prod

# Base de Datos PostgreSQL 16
DB_NAME=pictohabla
DB_USER=pictohabla_app
DB_PASSWORD=<CONTRASEÑA_BD_SEGURA_GENERADA_CON_OPENSSL>

# Seguridad JWT (mínimo 256 bits en hexadecimal)
JWT_SECRET=<CLAVE_JWT_HEX_64_CHARS_GENERADA_CON_OPENSSL>
JWT_ACCESS_EXPIRATION_MINUTES=15
JWT_REFRESH_EXPIRATION_DAYS=7

# Control de Orígenes CORS (Dominios autorizados sin barra final)
CORS_ALLOWED_ORIGINS=https://pictohabla.local,https://<TU_DOMINIO_PUBLICO>

# URL Pública de la API en Cloudflare Tunnel
PUBLIC_API_URL=https://api.<TU_DOMINIO_PUBLICO>

# Subred de Confianza de Cloudflare Tunnel
TRUSTED_PROXY_SUBNET=172.16.1.0/24

# Verificación de Email en Registro de Profesores
EMAIL_VERIFICATION_REQUIRED=false

# Configuración SMTP Transaccional (Opcional - dejar vacío si no se usa)
SMTP_HOST=
SMTP_PORT=587
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_FROM=no-reply@<TU_DOMINIO_PUBLICO>

# Clave opcional para cifrado simétrico GPG de copias (.dump.gpg)
BACKUP_ENCRYPTION_KEY=
```

### Comportamiento del Sistema sin Servidor SMTP Configurado:
El backend está programado con inyección opcional de `JavaMailSender` (`@Autowired(required = false)`):
1. **Registro de Profesores (`EMAIL_VERIFICATION_REQUIRED=false`):**
   Los profesores se registran y su cuenta queda inmediatamente activa sin requerir confirmación por correo electrónico.
2. **Protección contra Abuso:**
   El endpoint `/auth/register` está protegido por `RateLimiterService` en memoria (máximo **10 registros por hora por IP**).
3. **Recuperación de Contraseña (`/auth/forgot-password`):**
   Si se invoca sin SMTP configurado, devuelve una respuesta HTTP `200 OK` genérica con el mensaje *"Si el correo está registrado, se enviará un enlace de recuperación"* para evitar ataques de enumeración de cuentas. El backend emite una advertencia en el log del servidor indicando que el correo no pudo enviarse debido a la ausencia de servidor SMTP.

---

## 🌐 6. Aislamiento de Red y Verificación de Subred

### Topología de Red de PictoHabla:
- **`pictohabla-internal` (driver: bridge, internal: true):**
  Red privada aislada sin salida a internet ni puertos expuestos. Conecta exclusivamente el contenedor `backend` con `postgres`. PostgreSQL **no** forma parte de la red de Cloudflare y el puerto `5432` no se publica en el host.
- **`ix-cloudflared_default` (external: true):**
  Red Docker preexistente del contenedor `ix-cloudflared-cloudflared-1`. El backend se conecta a esta red con el alias DNS interno `pictohabla-backend`.

### Detección de Discrepancias de Subred:
El script `precheck-truenas.sh` comprueba que la subred real de `ix-cloudflared_default` (obtenida dinámicamente con `docker network inspect`) coincida exactamente con `TRUSTED_PROXY_SUBNET` del `.env`.
> ⚠️ **Por qué es crítico:** Spring Boot utiliza `ForwardedHeaderFilter` para validar las cabeceras `X-Forwarded-For` y `X-Forwarded-Proto`. Si la IP del proxy de Cloudflare no pertenece a la subred de confianza declarada, Spring Boot descartará las cabeceras por seguridad, provocando fallos de CORS, redirecciones HTTP incorrectas y pérdida de la IP real en los registros de auditoría.

---

## 🧪 7. Pruebas de Conectividad con Contenedor Efímero de Diagnóstico

### Por qué falla `docker exec ix-cloudflared-cloudflared-1 curl ...`:
La imagen oficial de Cloudflare Tunnel (`cloudflare/cloudflared`) se distribuye compilada sobre una base **scratch / distroless** sin sistema operativo completo. **No contiene `/bin/sh`, `/bin/bash` ni la utilidad `curl`**. Ejecutar `docker exec` contra ese contenedor fallará siempre con error `OCI runtime exec failed: exec: "curl": executable file not found in $PATH`.

### Procedimiento Oficial No Invasivo (Contenedor Efímero):
Para probar la resolución DNS interna y la conectividad HTTP entre Cloudflare Tunnel y el backend sin alterar ningún contenedor en producción, ejecuta un contenedor temporal conectado a `ix-cloudflared_default`:

```bash
docker run --rm --network ix-cloudflared_default curlimages/curl:latest curl -s -I http://pictohabla-backend:8080/actuator/health
```

#### Salida Esperada:
```http
HTTP/1.1 200 OK
Content-Type: application/vnd.spring-boot.actuator.v3+json
...
```

Este comando verifica:
1. Resolución DNS interna de Docker (`pictohabla-backend` resuelve a la IP interna del contenedor backend).
2. Conectividad TCP directa en el puerto `8080` a través de la red de Cloudflare.
3. El contenedor se destruye automáticamente (`--rm`) sin dejar residuos en el sistema.

---

## 💾 8. Límites de Memoria y Detección de OOM (Out Of Memory)

### Configuración de Recursos en `compose.yaml`:
```yaml
mem_limit: 512m
deploy:
  resources:
    limits:
      memory: 512M
    reservations:
      memory: 256M
```

### Mecánica del Límite de Memoria:
- `mem_limit: 512m` es un **límite estricto de cgroups** impuesto por el kernel de Linux. Si el proceso Java consume 513 MB en total, el kernel Linux interviene inmediatamente y finaliza el proceso con la señal SIGKILL (**Código de salida 137**).
- Para evitar esto, se configura la JVM en `JAVA_OPTS`:
  ```bash
  JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
  ```
  - `-XX:MaxRAMPercentage=75.0`: Reserva el 75% de 512 MB = **384 MB para el Heap** de Java.
  - El 25% restante (**128 MB**) queda reservado para Metaspace, memoria nativa de subprocesos (threads), buffers de socket y la sobrecarga del recolector de basura (GC).
  - `-XX:+ExitOnOutOfMemoryError`: Fuerza un volcado y salida controlada si el Heap se agota, evitando que el contenedor quede bloqueado en un bucle infinito de recolección de basura.

### Cómo Diagnosticar si el Contenedor Sufrió un OOM Kill:
Ejecuta en la shell de TrueNAS:
```bash
# Comprobar si el contenedor fue eliminado por el OOM Killer del kernel:
docker inspect pictohabla-backend --format 'OOMKilled: {{.State.OOMKilled}}, ExitCode: {{.State.ExitCode}}'

# Revisar los mensajes del kernel de Linux:
dmesg -T | grep -i -E "oom[- ]killer|pictohabla|java"
```
Si `OOMKilled` es `true` o el código de salida es `137`, se debe revisar el consumo de memoria o ajustar el heap.

---

## 📜 9. Scripts de Mantenimiento con Bash y PATH Explícito

Todos los scripts (`backup.sh`, `restore.sh`, `precheck-truenas.sh`) están normalizados con:
- Intérprete estricto `#!/bin/bash`.
- Manejo de fallos con `set -euo pipefail` (o `set -u`).
- Exportación explícita de `PATH` para ejecución segura y desatendida desde el demonio cron de TrueNAS:
  ```bash
  export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${PATH:-}"
  ```

### Copia de Scripts a TrueNAS:
```bash
mkdir -p /mnt/Datos/PictoHabla/scripts
cp backend/scripts/*.sh /mnt/Datos/PictoHabla/scripts/
chmod 700 /mnt/Datos/PictoHabla/scripts/backup.sh
chmod 700 /mnt/Datos/PictoHabla/scripts/restore.sh
chmod 755 /mnt/Datos/PictoHabla/scripts/precheck-truenas.sh
```

### Configuración del Cron Job Diario en TrueNAS SCALE:
En la interfaz web de TrueNAS SCALE (**System Settings** -> **Advanced** -> **Cron Jobs** -> **Add**):
- **Description:** `PictoHabla - Backup Diario PostgreSQL`
- **Command:** `/mnt/Datos/PictoHabla/scripts/backup.sh >> /var/log/pictohabla-backup.log 2>&1`
- **Run as User:** `root`
- **Schedule:** Diario a las `03:00` (hora de bajo uso).

### Procedimiento de Restauración Controlada:
El script `restore.sh` protege la base de datos de sobreescrituras accidentales:
1. Requiere escribir textualmente `CONFIRMAR_RESTAURACION`.
2. Verifica la integridad del dump mediante `pg_restore --list`.
3. Detiene temporalmente `pictohabla-backend` para evitar bloqueos y transacciones huérfanas.
4. Cierra conexiones activas en PostgreSQL.
5. Ejecuta `pg_restore` con `-c --if-exists --clean --no-owner --no-privileges`.
6. Regenera estadísticas de consulta con `ANALYZE`.
7. Reinicia el contenedor del backend.

Ejecución manual de restauración:
```bash
bash /mnt/Datos/PictoHabla/scripts/restore.sh /mnt/Seguridad/PictoHabla/Backups/pictohabla_pictohabla_YYYYMMDD_HHMMSS.dump
```

---

## 🎯 10. Método Primario Recomendado: Docker Compose CLI vía SSH

### Análisis Comparativo de Despliegue:

| Criterio | Docker Compose CLI vía SSH (Recomendado) | TrueNAS Custom App (UI) |
| :--- | :--- | :--- |
| **Control de Configuración** | **Total y determinista.** Lee directamente el archivo `.env` estándar sin intermediarios. | Parcial. Requiere introducir variables a mano en formularios web propensos a errores. |
| **Redes Externas** | **Nativo y sin fricción.** Conecta a `ix-cloudflared_default` de forma inmediata. | A menudo falla o recrea redes si no fueron registradas previamente por la UI. |
| **Diagnóstico y Logs** | **Inmediato.** `docker compose logs -f` muestra la salida completa de Flyway y Spring. | Interfaz web paginada con truncamiento de logs en arranques largos. |
| **Integración con Scripts** | **Directa.** Compatible 100% con los scripts de backup, restore y cron del sistema. | Los scripts externos no quedan vinculados al ciclo de vida de la app en la UI. |
| **Reproducibilidad** | **Alta.** Un simple `docker compose pull && docker compose up -d` gestiona todo. | Requiere navegación por múltiples pantallas del panel de control. |

### Decisión de Arquitectura:
El método **Docker Compose CLI vía SSH** es la opción recomendada y primaria para este despliegue.

---

## 🚀 11. Procedimiento de Despliegue Paso a Paso

### Paso 1: Ejecutar la Auditoría Pre-vuelo
Conéctate por SSH a TrueNAS y ejecuta:
```bash
bash /mnt/Datos/PictoHabla/scripts/precheck-truenas.sh
```
Verifica que finalice con: `✔ TODAS LAS COMPROBACIONES SUPERADAS SIN ERRORES NI ADVERTENCIAS.`

### Paso 2: Construir o Cargar la Imagen Docker

#### Opción A — Construcción en TrueNAS (Recomendada si el código está en `/mnt/Datos/PictoHabla/build`):
```bash
cd /mnt/Datos/PictoHabla/build
docker build -t pictohabla-backend:1.0.0 .
```

#### Opción B — Carga de imagen transferida desde el equipo de desarrollo:
```bash
docker load -i /mnt/Datos/PictoHabla/pictohabla-backend-1.0.0.tar.gz
```

### Paso 3: Preparar el Directorio de Despliegue
Copia `compose.truenas.yaml` como `compose.yaml` en `/mnt/Datos/PictoHabla/`:
```bash
cp /mnt/Datos/PictoHabla/build/compose.truenas.yaml /mnt/Datos/PictoHabla/compose.yaml
```

Verifica que `/mnt/Datos/PictoHabla/.env` existe, tiene permisos `600` y sus contraseñas generadas.

### Paso 4: Iniciar los Contenedores
```bash
cd /mnt/Datos/PictoHabla
docker compose --env-file .env -f compose.yaml up -d
```

### Paso 5: Verificar Migraciones de Flyway y Salud
```bash
# Ver estado de los contenedores:
docker compose ps

# Comprobar que Flyway ejecutó las migraciones correctamente:
docker logs pictohabla-backend | grep -i "flyway"
```
Salida esperada:
```
Successfully applied 1 migration to schema "public", now at version v1 (initial schema)
```

### Paso 6: Validar Conectividad con el Contenedor Efímero
```bash
docker run --rm --network ix-cloudflared_default curlimages/curl:latest curl -s -I http://pictohabla-backend:8080/actuator/health
```

### Paso 7: Configurar Cloudflare Zero Trust Dashboard
1. Accede a **Cloudflare Zero Trust** (`one.dash.cloudflare.com`) -> **Networks** -> **Tunnels**.
2. Edita el túnel activo de TrueNAS y añade un **Public Hostname**:
   - **Subdomain:** `api`
   - **Domain:** `<TU_DOMINIO_PUBLICO>`
   - **Path:** (dejar vacío)
   - **Service Type:** `HTTP`
   - **URL:** `pictohabla-backend:8080`
3. Guarda el hostname.
4. Verifica el acceso desde tu navegador o terminal:
   ```bash
   curl -I https://api.<TU_DOMINIO_PUBLICO>/actuator/health
   ```

---

## 📊 12. Estado de Pruebas Automatizadas

El backend de PictoHabla cuenta con **37 pruebas unitarias y de integración** totalmente satisfactorias (0 errores, 0 fallos, 0 omitidas):

```
BUILD SUCCESSFUL in 18s
37 tests completed, 0 failed, 0 skipped
```

- `SecurityIntegrationTest` (6 pruebas): Flujos JWT, acceso anónimo a catálogo, protección de endpoints de profesores, expiración y refresh tokens.
- `AuthControllerTest` (8 pruebas): Registro, login, refresh, logout, formato de errores y rate-limiting.
- `TeacherServiceTest` (5 pruebas): Lógica de negocio de creación, búsqueda y unicidad de profesores.
- `JwtServiceTest` (4 pruebas): Generación criptográfica HMAC-SHA256, validación de claims y expiraciones.
- `PasswordServiceTest` (3 pruebas): Hash Argon2 / BCrypt y verificación de entropía.
- `RateLimiterServiceTest` (4 pruebas): Cuotas por IP y reseteo temporal.
- `StorageServiceTest` (4 pruebas): Gestión de archivos multimedia y validación de tipos MIME seguros.
- `PictoHablaApplicationTests` (3 pruebas): Inicialización de contexto Spring Boot, perfiles y datasource.
