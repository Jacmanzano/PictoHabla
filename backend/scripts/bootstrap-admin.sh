#!/bin/bash
# ==============================================================================
# PictoHabla — Script de Bootstrap Seguro del Primer Administrador (One-Shot)
# ==============================================================================
# Este script ejecuta un contenedor efímero aislado (--rm) sin iniciar servidor
# HTTP permanente ni abrir ningún puerto de red hacia el exterior.
# La contraseña se solicita mediante entrada oculta (read -s) para evitar que
# quede registrada en el historial de comandos (.bash_history) o procesos visibles.
# ==============================================================================

set -euo pipefail

# Asegurar PATH completo para ejecución en TrueNAS SCALE
export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${PATH:-}"

ENV_FILE="/mnt/Datos/Aplicaciones/PictoHabla/.env"
if [ ! -f "${ENV_FILE}" ] && [ -f "/mnt/Datos/PictoHabla/.env" ]; then
    ENV_FILE="/mnt/Datos/PictoHabla/.env"
fi

if [ ! -f "${ENV_FILE}" ]; then
    echo "[ERROR CRÍTICO] Archivo de entorno ${ENV_FILE} no encontrado." >&2
    exit 1
fi

IMAGE_NAME="pictohabla-backend:1.0.1"

# Red interna real de TrueNAS SCALE Apps para Custom App 'pictohabla'
NETWORK_NAME="ix-pictohabla_pictohabla-internal"
if ! docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
    echo "[ERROR CRÍTICO] La red Docker '${NETWORK_NAME}' no fue encontrada en el sistema." >&2
    echo "Verifica que la Custom App 'pictohabla' está desplegada y en ejecución en TrueNAS SCALE." >&2
    exit 1
fi

echo "=============================================================================="
echo " 🛡️ PictoHabla — Creación Segura del Primer Administrador (Bootstrap)"
echo "=============================================================================="
echo "Servidor de destino : TrueNAS SCALE 25.10.7"
echo "Imagen utilizada    : ${IMAGE_NAME}"
echo "Red Docker interna  : ${NETWORK_NAME}"
echo "Modo de ejecución   : Tarea aislada (WebApplicationType.NONE, sin servidor HTTP)"
echo "Transmisión segura  : Flujo cifrado por entrada estándar (stdin, sin exposición en inspect/ps)"
echo "=============================================================================="

# Solicitar credenciales interactivamente sin eco en pantalla ni historial bash
read -r -p "Introduce el correo del Administrador: " ADMIN_EMAIL
read -r -p "Introduce el nombre completo del Administrador: " ADMIN_FULL_NAME
read -s -r -p "Introduce la contraseña (oculta, mín 8 caracteres con letra y número): " ADMIN_PASSWORD
echo ""
read -s -r -p "Confirma la contraseña: " ADMIN_PASSWORD_CONFIRM
echo ""

if [ "${ADMIN_PASSWORD}" != "${ADMIN_PASSWORD_CONFIRM}" ]; then
    echo "[ERROR] Las contraseñas introducidas no coinciden. Operación abortada." >&2
    unset ADMIN_EMAIL ADMIN_FULL_NAME ADMIN_PASSWORD ADMIN_PASSWORD_CONFIRM
    exit 1
fi

if [ -z "${ADMIN_EMAIL}" ] || [ -z "${ADMIN_PASSWORD}" ] || [ -z "${ADMIN_FULL_NAME}" ]; then
    echo "[ERROR] Todos los campos son obligatorios. Operación abortada." >&2
    unset ADMIN_EMAIL ADMIN_FULL_NAME ADMIN_PASSWORD ADMIN_PASSWORD_CONFIRM
    exit 1
fi

echo "[INFO] Conectando de forma segura con PostgreSQL en red interna para crear administrador..."

# Transmisión segura por stdin:
# - Se evita pasar la contraseña como variable de entorno (-e ADMIN_PASSWORD=...),
#   eliminando cualquier visibilidad en 'docker inspect' o 'ps' del host.
# - El proceso Java lee email, fullName y password secuencialmente desde System.in.
# - Se inyectan explícitamente las variables de conexión a PostgreSQL idénticas al servicio Compose.
printf '%s\n%s\n%s\n' "${ADMIN_EMAIL}" "${ADMIN_FULL_NAME}" "${ADMIN_PASSWORD}" | docker run --rm -i \
    --network "${NETWORK_NAME}" \
    --env-file "${ENV_FILE}" \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e DB_HOST=postgres \
    -e DB_PORT=5432 \
    --entrypoint java \
    "${IMAGE_NAME}" \
    -Dspring.main.web-application-type=none \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -jar app.jar \
    --pictohabla.bootstrap.admin=true

EXIT_CODE=$?

# Borrar variables en memoria de la sesión
unset ADMIN_EMAIL ADMIN_FULL_NAME ADMIN_PASSWORD ADMIN_PASSWORD_CONFIRM

if [ ${EXIT_CODE} -eq 0 ]; then
    echo "[INFO] Operación de bootstrap finalizada. Variables de memoria limpiadas."
else
    echo "[ERROR] El contenedor de bootstrap finalizó con código de error ${EXIT_CODE}." >&2
fi

exit ${EXIT_CODE}
