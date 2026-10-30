# 🚀 Guía de Despliegue en Producción — TrueNAS SCALE Community 25.10.7

Esta guía describe el procedimiento oficial, seguro y reproducible para desplegar el backend de **PictoHabla** y su base de datos **PostgreSQL 16** en **TrueNAS SCALE Community 25.10.7**, integrándose con la infraestructura existente de **Cloudflare Tunnel** (`ix-cloudflared_default`) sin publicar puertos en el host y sin comprometer las demás aplicaciones en ejecución.

---

## 📋 1. Requisitos Previos e Infraestructura

### Hardware del Servidor
- **Procesador:** Intel Xeon E3-1225 v6 (arquitectura `x86_64` / `amd64`).
- **Memoria RAM:** 32 GB ECC (se reservan 768 MB para PictoHabla: 256 MB para PostgreSQL y 512 MB para Spring Boot).
- **Almacenamiento:** ZFS en dos pools (`Datos` y `Seguridad`).  
  > ⚠️ **Aviso de Resiliencia:** Los pools están compuestos por discos individuales sin espejo/redundancia física. Las copias de seguridad lógicas son esenciales, pero se recomienda configurar periódicamente tareas de sincronización (Cloud Sync Tasks) hacia un almacenamiento externo o nube.

### Aplicaciones Coexistentes (No Modificar)
El servidor ejecuta aplicaciones en producción que **deben permanecer intactas**:
- `cloudflared` (en contenedor `ix-cloudflared-cloudflared-1`, red `ix-cloudflared_default`, subred `172.16.1.0/24`).
- `tailscale`, `pihole`, `beszel`, `immich`, `vaultwarden`, `n8n`.

---

## 🗄️ 2. Preparación de Datasets ZFS y Permisos

Accede a la interfaz web de TrueNAS SCALE (**Datasets**) o ejecuta vía SSH:

### 2.1. Comprobación y Creación de Datasets

1. **PostgreSQL** (en pool `Datos`):
   - Ruta: `/mnt/Datos/PictoHabla/Postgres`
   - Configuración ZFS recomendada: `recordsize=16k` (óptimo para páginas de Postgres), `compression=zstd`.
   - Permisos: UID `70`, GID `70` (usuario `postgres` en Alpine Linux).
   ```bash
   # Comprobar o ajustar permisos si el dataset está recién creado:
   chown 70:70 /mnt/Datos/PictoHabla/Postgres
   chmod 700 /mnt/Datos/PictoHabla/Postgres
   ```

2. **Subidas de Archivos** (en pool `Datos`):
   - Ruta: `/mnt/Datos/PictoHabla/Uploads`
   - Configuración ZFS recomendada: `recordsize=128k`, `compression=zstd`.
   - Permisos: UID `1000`, GID `1000` (usuario `spring` dentro del contenedor).
   ```bash
   chown 1000:1000 /mnt/Datos/PictoHabla/Uploads
   chmod 750 /mnt/Datos/PictoHabla/Uploads
   ```

3. **Copias de Seguridad** (en pool `Seguridad`):
   - Ruta: `/mnt/Seguridad/PictoHabla/Backups`
   - Si no está creado, créalo desde la UI de TrueNAS (**Datasets** -> **Add Dataset** en pool `Seguridad`).
   - Permisos: Acceso exclusivo para root / tareas del sistema (`chmod 700`).
   ```bash
   mkdir -p /mnt/Seguridad/PictoHabla/Backups
   chmod 700 /mnt/Seguridad/PictoHabla/Backups
   ```

4. **Directorio de Scripts**:
   - Ruta: `/mnt/Datos/PictoHabla/scripts`
   ```bash
   mkdir -p /mnt/Datos/PictoHabla/scripts
   ```

> 🛑 **Regla de Seguridad:** NUNCA ejecutes `chmod -R 777` ni modifiques permisos en la raíz de los pools.

---

## 🔍 3. Comprobaciones Previas (Script de Pre-vuelo)

Copia el script de auditoría previa a TrueNAS y ejecútalo para verificar el entorno:

```bash
# En TrueNAS SCALE:
sh /mnt/Datos/PictoHabla/scripts/precheck-truenas.sh
```

El script verificará:
1. Docker y Docker Compose activos.
2. Existencia de los 3 datasets y permisos compatibles.
3. Existencia de la red `ix-cloudflared_default` y contenedor `cloudflared`.
4. Memoria RAM libre (> 1 GB) y espacio disponible en disco.
5. Inexistencia de conflictos de puertos en el host.

---

## 🐳 4. Construcción y Carga de la Imagen Docker

La imagen `pictohabla-backend:1.0.0` debe estar disponible en el daemon Docker de TrueNAS. Tienes dos opciones:

### Opción A: Construcción directa en TrueNAS SCALE (Recomendada)
Si copias el código de `backend/` a `/mnt/Datos/PictoHabla/src-build/`:
```bash
cd /mnt/Datos/PictoHabla/src-build
docker build -t pictohabla-backend:1.0.0 -f Dockerfile .
```
*El Dockerfile multi-stage descarga eclipse-temurin:21, compila el JAR con el Gradle wrapper incluido y genera la imagen final no-root de producción.*

### Opción B: Construcción en equipo de desarrollo y transferencia por tar
Desde un equipo con Docker y arquitectura x86_64:
```bash
# Construir para la arquitectura del Xeon E3 (linux/amd64):
docker buildx build --platform linux/amd64 -t pictohabla-backend:1.0.0 -f Dockerfile .

# Exportar a archivo tar:
docker save pictohabla-backend:1.0.0 | gzip > pictohabla-backend-1.0.0.tar.gz

# Transferir a TrueNAS vía SCP / SFTP:
scp pictohabla-backend-1.0.0.tar.gz admin@truenas.local:/mnt/Datos/PictoHabla/

# En TrueNAS SCALE, cargar la imagen en Docker:
docker load -i /mnt/Datos/PictoHabla/pictohabla-backend-1.0.0.tar.gz
```

---

## 🔐 5. Configuración de Variables de Entorno (`.env`)

En TrueNAS, crea el archivo `/mnt/Datos/PictoHabla/.env` a partir de `.env.truenas.example` con permisos restringidos:

```bash
cd /mnt/Datos/PictoHabla
touch .env
chmod 600 .env
```

Genera contraseñas criptográficas seguras:
```bash
# Generar contraseña de PostgreSQL:
openssl rand -hex 32

# Generar clave secreta para firma JWT (mínimo 256 bits):
openssl rand -hex 32
```

Contenido de `/mnt/Datos/PictoHabla/.env`:
```env
SPRING_PROFILES_ACTIVE=prod
DB_NAME=pictohabla
DB_USER=pictohabla_app
DB_PASSWORD=<CONTRASEÑA_GENERADA_CON_OPENSSL>

JWT_SECRET=<CLAVE_JWT_HEX_GENERADA_CON_OPENSSL>
JWT_ACCESS_EXPIRATION_MINUTES=15
JWT_REFRESH_EXPIRATION_DAYS=7

CORS_ALLOWED_ORIGINS=https://pictohabla.local,https://tudominio.com
PUBLIC_API_URL=https://api.tudominio.com
TRUSTED_PROXY_SUBNET=172.16.1.0/24
EMAIL_VERIFICATION_REQUIRED=false

# Opcional para copias cifradas:
BACKUP_ENCRYPTION_KEY=
```

---

## 🚀 6. Despliegue con Docker Compose en TrueNAS SCALE

### Método 1: Vía Shell / SSH (Rápido y Transparente)

Copia `compose.truenas.yaml` a `/mnt/Datos/PictoHabla/compose.yaml`:

```bash
cd /mnt/Datos/PictoHabla
docker compose --env-file .env -f compose.yaml up -d
```

### Método 2: Vía Interfaz Web de TrueNAS SCALE 25.10.7
1. Ve a **Apps** -> **Discover Apps** -> botón **Custom App** (arriba a la derecha).
2. Selecciona **Install Custom App using Docker Compose**.
3. Nombre de la aplicación: `pictohabla`.
4. Pega el contenido de `compose.truenas.yaml`.
5. En la sección de Environment / Variables, especifica las variables o asegúrate de que lee el archivo de entorno.
6. Pulsa **Install**.

---

## 🩺 7. Verificación de Salud y Pruebas de Aceptación

### 7.1. Comprobar Estado de los Contenedores
```bash
docker ps -f name=pictohabla
```
Deben aparecer ambos contenedores en estado `Up` y `(healthy)`.

### 7.2. Comprobar Migraciones Flyway en Logs
```bash
docker logs pictohabla-backend | grep -i "flyway"
```
Salida esperada:
```
Successfully applied 1 migration to schema "public", now at version v1 (initial schema)
```

### 7.3. Comprobar Endpoint Actuator Health
Desde el propio host del NAS (o dentro de la red Docker):
```bash
docker exec pictohabla-backend curl -s http://127.0.0.1:8080/actuator/health
```
Respuesta esperada:
```json
{"status":"UP"}
```

### 7.4. Comprobar Resolución DNS y Conectividad desde Cloudflare Tunnel
Desde el contenedor de Cloudflare Tunnel:
```bash
docker exec ix-cloudflared-cloudflared-1 curl -s -I http://pictohabla-backend:8080/actuator/health
```
Salida esperada:
```
HTTP/1.1 200 OK
```

---

## ☁️ 8. Configuración en Cloudflare Zero Trust Dashboard

Para publicar la API de forma segura sin abrir puertos en el router:

1. Inicia sesión en el panel de **Cloudflare Zero Trust** (`one.dash.cloudflare.com`).
2. Ve a **Networks** -> **Tunnels** -> Selecciona tu túnel activo de TrueNAS.
3. Añade un nuevo **Public Hostname**:
   - **Subdomain:** `api` (o el subdominio que elijas, ej. `api.pictohabla.com`).
   - **Domain:** Tu dominio gestionado en Cloudflare.
   - **Path:** Vacío (aplica a toda la API).
   - **Service Type:** `HTTP`.
   - **URL:** `pictohabla-backend:8080`.
4. En **Additional application settings**:
   - **HTTP Settings:** HTTP2 habilitado.
   - **No TLS Verify:** No requerido (la comunicación backend-tunnel es HTTP pura en red interna).
5. Pulsa **Save Hostname**.

---

## 💾 9. Copias de Seguridad y Restauración

Copia los scripts a `/mnt/Datos/PictoHabla/scripts/`:
- `backup.sh`
- `restore.sh`
```bash
chmod +x /mnt/Datos/PictoHabla/scripts/*.sh
```

### 9.1. Tarea Cron Diaria en TrueNAS SCALE
En la interfaz web de TrueNAS SCALE (**System Settings** -> **Advanced** -> **Cron Jobs**):
- **Description:** `PictoHabla - Backup diario PostgreSQL`
- **Command:** `/mnt/Datos/PictoHabla/scripts/backup.sh >> /var/log/pictohabla-backup.log 2>&1`
- **Run as User:** `root`
- **Schedule:** Diario a las `03:00` (hora de baja actividad).

### 9.2. Ejecución Manual de Copia de Seguridad
```bash
sh /mnt/Datos/PictoHabla/scripts/backup.sh
```
El script genera un archivo `pictohabla_pictohabla_YYYYMMDD_HHMMSS.dump` en `/mnt/Seguridad/PictoHabla/Backups/`, verifica su integridad con `pg_restore --list` y aplica la retención automática de 30 días.

### 9.3. Procedimiento de Restauración Controlada
Si se requiere recuperar un estado previo:
```bash
sh /mnt/Datos/PictoHabla/scripts/restore.sh /mnt/Seguridad/PictoHabla/Backups/pictohabla_pictohabla_YYYYMMDD_HHMMSS.dump
```
> ⚠️ **Protección:** El script exige confirmación explícita escribiendo `CONFIRMAR_RESTAURACION`, detiene temporalmente el backend para evitar estados inconsistentes, ejecuta `pg_restore`, regenera estadísticas con `ANALYZE` y reinicia el servicio.

---

## 🔄 10. Procedimiento de Actualización y Rollback

### 10.1. Actualización de Versión
1. Construir o cargar la nueva imagen: `pictohabla-backend:1.1.0`.
2. Actualizar la etiqueta en `compose.yaml`: `image: pictohabla-backend:1.1.0`.
3. Ejecutar copia preventiva: `sh /mnt/Datos/PictoHabla/scripts/backup.sh`.
4. Aplicar la actualización:
   ```bash
   docker compose --env-file .env -f compose.yaml up -d --no-deps backend
   ```
5. Comprobar logs de Flyway: `docker logs -f pictohabla-backend`.

### 10.2. Rollback
Si la nueva versión presenta problemas:
1. Revertir la imagen en `compose.yaml` a la versión anterior (`1.0.0`).
2. Reiniciar el contenedor:
   ```bash
   docker compose --env-file .env -f compose.yaml up -d --no-deps backend
   ```
3. Si la base de datos sufrió cambios incompatibles hacia atrás, restaurar el dump preventivo previo mediante `restore.sh`.
