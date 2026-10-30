#!/bin/sh
# ==============================================================================
# PictoHabla — Script de Restauración Controlada de Base de Datos PostgreSQL
# ==============================================================================
# ADVERTENCIA: Este script sobreescribe los datos existentes en la base de datos.
# NUNCA se debe ejecutar automáticamente. Requiere intervención consciente del
# administrador de TrueNAS SCALE.
# Ubicación recomendada en TrueNAS: /mnt/Datos/PictoHabla/scripts/restore.sh
# ==============================================================================

set -eu

if [ $# -lt 1 ]; then
    echo "Uso: $0 <ruta_al_archivo_dump>"
    echo "Ejemplo: $0 /mnt/Seguridad/PictoHabla/Backups/pictohabla_pictohabla_20261030_030000.dump"
    exit 1
fi

BACKUP_FILE="$1"
CONTAINER_NAME="pictohabla-postgres"
BACKEND_CONTAINER="pictohabla-backend"
DB_USER="${DB_USER:-pictohabla_app}"
DB_NAME="${DB_NAME:-pictohabla}"

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "[ERROR] El archivo de copia de seguridad no existe: ${BACKUP_FILE}" >&2
    exit 1
fi

echo "=============================================================================="
echo "⚠️  ADVERTENCIA DE SEGURIDAD — RESTAURACIÓN DE BASE DE DATOS PICTOHABLA"
echo "=============================================================================="
echo "Base de datos destino : ${DB_NAME}"
echo "Usuario PostgreSQL    : ${DB_USER}"
echo "Contenedor BD         : ${CONTAINER_NAME}"
echo "Contenedor Backend    : ${BACKEND_CONTAINER}"
echo "Archivo de origen     : ${BACKUP_FILE} ($(du -h "${BACKUP_FILE}" | cut -f1))"
echo "=============================================================================="
echo "Esta operación reemplazará todas las tablas y datos actuales de la base de datos."
echo "Para proceder, escribe exactamente 'CONFIRMAR_RESTAURACION':"
read -r CONFIRMATION

if [ "${CONFIRMATION}" != "CONFIRMAR_RESTAURACION" ]; then
    echo "Operación cancelada por el usuario. No se ha modificado ningún dato."
    exit 0
fi

# Verificar integridad del dump antes de tocar la base de datos
echo "[INFO] Verificando integridad del archivo dump..."
if ! docker exec -i "${CONTAINER_NAME}" pg_restore --list < "${BACKUP_FILE}" > /dev/null 2>&1; then
    echo "[ERROR] El archivo de copia está corrupto o no corresponde al formato custom de pg_dump." >&2
    exit 1
fi
echo "[OK] Integridad del archivo verificada."

# Detener temporalmente el backend para evitar escrituras concurrentes durante la restauración
echo "[INFO] Deteniendo contenedor backend (${BACKEND_CONTAINER}) para evitar escrituras concurrentes..."
docker stop "${BACKEND_CONTAINER}" 2>/dev/null || true

# Terminar conexiones activas hacia la base de datos antes de restaurar
echo "[INFO] Cerrando sesiones activas en ${DB_NAME}..."
docker exec -e DB_NAME="${DB_NAME}" "${CONTAINER_NAME}" \
    psql -U "${DB_USER}" -d postgres -c \
    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '${DB_NAME}' AND pid <> pg_backend_pid();" > /dev/null 2>&1 || true

# Ejecutar restauración utilizando pg_restore con limpieza de objetos previos (-c --if-exists)
echo "[INFO] Restaurando datos en ${DB_NAME}..."
docker exec -i -e DB_USER="${DB_USER}" -e DB_NAME="${DB_NAME}" "${CONTAINER_NAME}" \
    sh -c 'pg_restore -U "$DB_USER" -d "$DB_NAME" -c --if-exists --clean --no-owner --no-privileges' < "${BACKUP_FILE}" || true

# Ejecutar ANALYZE para regenerar las estadísticas del optimizador de consultas
echo "[INFO] Regenerando estadísticas del optimizador (ANALYZE)..."
docker exec -e DB_USER="${DB_USER}" -e DB_NAME="${DB_NAME}" "${CONTAINER_NAME}" \
    psql -U "${DB_USER}" -d "${DB_NAME}" -c "ANALYZE;"

# Reiniciar el backend
echo "[INFO] Iniciando nuevamente el contenedor backend (${BACKEND_CONTAINER})..."
docker start "${BACKEND_CONTAINER}"

echo "=============================================================================="
echo "✅  RESTAURACIÓN COMPLETADA SATISFACTORIAMENTE"
echo "=============================================================================="
