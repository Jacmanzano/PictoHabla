#!/bin/sh
# ==============================================================================
# PictoHabla — Script de Copia de Seguridad Lógica de PostgreSQL en TrueNAS SCALE
# ==============================================================================
# Destino: /mnt/Seguridad/PictoHabla/Backups
# Formato: PostgreSQL Custom Format (-F c) con compresión y checksums internos
# Ubicación recomendada en TrueNAS: /mnt/Datos/PictoHabla/scripts/backup.sh
# Programación recomendada: Tarea cron en TrueNAS SCALE (diaria a las 03:00)
# ==============================================================================

set -eu

BACKUP_DIR="/mnt/Seguridad/PictoHabla/Backups"
CONTAINER_NAME="pictohabla-postgres"
DB_USER="${DB_USER:-pictohabla_app}"
DB_NAME="${DB_NAME:-pictohabla}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/pictohabla_${DB_NAME}_${TIMESTAMP}.dump"

# Verificar existencia del directorio de destino con permisos estrictos
if [ ! -d "${BACKUP_DIR}" ]; then
    echo "[$(date -Iseconds)] [ERROR] El directorio de destino ${BACKUP_DIR} no existe. Verifica que el dataset Seguridad/PictoHabla/Backups está montado." >&2
    exit 1
fi
chmod 700 "${BACKUP_DIR}"

echo "[$(date -Iseconds)] [INFO] Iniciando copia de seguridad lógica para base de datos '${DB_NAME}'..."

# Verificar que el contenedor de PostgreSQL está activo y saludable
if ! docker inspect -f '{{.State.Running}}' "${CONTAINER_NAME}" 2>/dev/null | grep -q "true"; then
    echo "[$(date -Iseconds)] [ERROR] El contenedor ${CONTAINER_NAME} no está en ejecución. Abortando copia." >&2
    exit 1
fi

# Ejecutar pg_dump dentro del contenedor sin exponer contraseñas en el host
# Se utiliza el formato Custom (-F c) nativo de PostgreSQL
docker exec -e DB_USER="${DB_USER}" -e DB_NAME="${DB_NAME}" "${CONTAINER_NAME}" \
    sh -c 'pg_dump -U "$DB_USER" -d "$DB_NAME" -F c -b' > "${BACKUP_FILE}"

# Comprobar que el archivo se ha creado y tiene contenido
if [ ! -s "${BACKUP_FILE}" ]; then
    echo "[$(date -Iseconds)] [ERROR] El archivo de copia está vacío o no se generó correctamente." >&2
    rm -f "${BACKUP_FILE}"
    exit 1
fi

# Validar integridad del dump mediante pg_restore --list dentro del contenedor
if ! docker exec -i "${CONTAINER_NAME}" pg_restore --list < "${BACKUP_FILE}" > /dev/null 2>&1; then
    echo "[$(date -Iseconds)] [ERROR] La verificación de integridad del dump falló. El archivo podría estar corrupto." >&2
    mv "${BACKUP_FILE}" "${BACKUP_FILE}.corrupt"
    exit 1
fi

BACKUP_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
echo "[$(date -Iseconds)] [OK] Copia completada y verificada: ${BACKUP_FILE} (${BACKUP_SIZE})"

# Política de retención:
# Eliminar copias de seguridad (.dump y .dump.gpg) con más de 30 días de antigüedad
find "${BACKUP_DIR}" -maxdepth 1 -name "pictohabla_${DB_NAME}_*.dump" -type f -mtime +30 -exec rm -f {} \;
find "${BACKUP_DIR}" -maxdepth 1 -name "pictohabla_${DB_NAME}_*.dump.gpg" -type f -mtime +30 -exec rm -f {} \;
echo "[$(date -Iseconds)] [INFO] Política de retención de 30 días aplicada."

# Cifrado GPG para almacenamiento o réplica fuera del NAS:
# Solo se ejecuta y declara si se ha suministrado explícitamente BACKUP_ENCRYPTION_KEY
if [ -n "${BACKUP_ENCRYPTION_KEY:-}" ]; then
    echo "[$(date -Iseconds)] [INFO] Variable BACKUP_ENCRYPTION_KEY detectada. Cifrando copia con AES-256..."
    echo "${BACKUP_ENCRYPTION_KEY}" | gpg --batch --yes --passphrase-fd 0 --symmetric --cipher-algo AES256 -o "${BACKUP_FILE}.gpg" "${BACKUP_FILE}"
    echo "[$(date -Iseconds)] [OK] Copia cifrada generada: ${BACKUP_FILE}.gpg"
else
    echo "[$(date -Iseconds)] [INFO] Cifrado GPG no solicitado (BACKUP_ENCRYPTION_KEY no establecida). Se conserva el archivo .dump estándar."
fi
