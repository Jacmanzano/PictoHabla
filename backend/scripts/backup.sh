#!/bin/bash
# ==============================================================================
# PictoHabla — Script de Copia de Seguridad Lógica de PostgreSQL en TrueNAS SCALE
# ==============================================================================
# Destino estricto: /mnt/Seguridad/PictoHabla/Backups
# Formato: PostgreSQL Custom Format (-F c) con compresión y checksums internos
# Ubicación en TrueNAS: /mnt/Datos/PictoHabla/scripts/backup.sh
# Programación: Tarea Cron en TrueNAS SCALE (diaria a las 03:00)
# ==============================================================================

set -euo pipefail

# Asegurar PATH completo para ejecución desatendida desde cron de TrueNAS
export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${PATH:-}"

BACKUP_DIR="/mnt/Seguridad/PictoHabla/Backups"
CONTAINER_NAME="pictohabla-postgres"
DB_USER="${DB_USER:-pictohabla_app}"
DB_NAME="${DB_NAME:-pictohabla}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/pictohabla_${DB_NAME}_${TIMESTAMP}.dump"

# 1. Comprobar que el dataset de copias de seguridad existe físicamente en el pool Seguridad
# Si no existe, abortar INMEDIATAMENTE para evitar escribir en una ruta errónea del sistema
if [ ! -d "${BACKUP_DIR}" ]; then
    echo "[$(date -Iseconds)] [ERROR CRÍTICO] El directorio de destino ${BACKUP_DIR} no existe." >&2
    echo "Debe existir el dataset ZFS 'Seguridad/PictoHabla/Backups' montado en esa ruta." >&2
    echo "Abortando copia de seguridad sin realizar modificaciones." >&2
    exit 1
fi

echo "[$(date -Iseconds)] [INFO] Iniciando copia de seguridad lógica para base de datos '${DB_NAME}'..."

# 2. Verificar que el contenedor de PostgreSQL está activo y saludable
if ! docker inspect -f '{{.State.Running}}' "${CONTAINER_NAME}" 2>/dev/null | grep -q "true"; then
    echo "[$(date -Iseconds)] [ERROR] El contenedor ${CONTAINER_NAME} no está en ejecución. Abortando copia." >&2
    exit 1
fi

# 3. Ejecutar pg_dump dentro del contenedor sin exponer contraseñas en el host
# Se utiliza el formato Custom (-F c) nativo de PostgreSQL
docker exec -e DB_USER="${DB_USER}" -e DB_NAME="${DB_NAME}" "${CONTAINER_NAME}" \
    sh -c 'pg_dump -U "$DB_USER" -d "$DB_NAME" -F c -b' > "${BACKUP_FILE}"

# 4. Comprobar que el archivo se ha creado y tiene contenido no nulo
if [ ! -s "${BACKUP_FILE}" ]; then
    echo "[$(date -Iseconds)] [ERROR] El archivo de copia está vacío o no se generó correctamente." >&2
    rm -f "${BACKUP_FILE}"
    exit 1
fi

# 5. Validar integridad del dump mediante pg_restore --list dentro del contenedor
if ! docker exec -i "${CONTAINER_NAME}" pg_restore --list < "${BACKUP_FILE}" > /dev/null 2>&1; then
    echo "[$(date -Iseconds)] [ERROR] La verificación de integridad del dump falló. El archivo podría estar corrupto." >&2
    mv "${BACKUP_FILE}" "${BACKUP_FILE}.corrupt"
    exit 1
fi

# Asegurar permisos restrictivos en el archivo generado
chmod 600 "${BACKUP_FILE}"

BACKUP_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
echo "[$(date -Iseconds)] [OK] Copia completada y verificada: ${BACKUP_FILE} (${BACKUP_SIZE})"

# 6. Política de retención:
# Eliminar copias de seguridad (.dump y .dump.gpg) con más de 30 días de antigüedad
find "${BACKUP_DIR}" -maxdepth 1 -name "pictohabla_${DB_NAME}_*.dump" -type f -mtime +30 -exec rm -f {} \;
find "${BACKUP_DIR}" -maxdepth 1 -name "pictohabla_${DB_NAME}_*.dump.gpg" -type f -mtime +30 -exec rm -f {} \;
echo "[$(date -Iseconds)] [INFO] Política de retención de 30 días aplicada."

# 7. Cifrado GPG opcional (si se configura BACKUP_ENCRYPTION_KEY)
if [ -n "${BACKUP_ENCRYPTION_KEY:-}" ]; then
    echo "[$(date -Iseconds)] [INFO] Variable BACKUP_ENCRYPTION_KEY detectada. Cifrando copia con AES-256..."
    echo "${BACKUP_ENCRYPTION_KEY}" | gpg --batch --yes --passphrase-fd 0 --symmetric --cipher-algo AES256 -o "${BACKUP_FILE}.gpg" "${BACKUP_FILE}"
    chmod 600 "${BACKUP_FILE}.gpg"
    echo "[$(date -Iseconds)] [OK] Copia cifrada generada: ${BACKUP_FILE}.gpg"
fi
