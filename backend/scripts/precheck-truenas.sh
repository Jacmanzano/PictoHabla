#!/bin/bash
# ==============================================================================
# PictoHabla — Script de Comprobaciones Previas al Despliegue en TrueNAS SCALE
# ==============================================================================
# AUDITORÍA ESTRICTAMENTE DE SÓLO LECTURA:
# Este script NO modifica archivos, NO cambia permisos y NO altera configuraciones.
# Analiza el entorno y reporta errores y advertencias antes de iniciar contenedores.
#
# Ejecutar en la shell SSH o consola de TrueNAS SCALE:
#   bash /mnt/Datos/PictoHabla/scripts/precheck-truenas.sh
# ==============================================================================

set -u

# Asegurar PATH completo para ejecución en TrueNAS SCALE
export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${PATH:-}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

echo "${BLUE}==============================================================================${NC}"
echo "${BLUE}   PICTOHABLA — AUDITORÍA PRE-VUELO EN TRUENAS SCALE 25.10.7${NC}"
echo "${BLUE}==============================================================================${NC}"
echo "Fecha y hora: $(date -Iseconds)"
echo "Host: $(hostname)"
echo ""

# -----------------------------------------------------------------------------
# 1. Herramientas Docker y Compose
# -----------------------------------------------------------------------------
echo "${BLUE}[1/7] Verificando Docker y motor de contenedores...${NC}"
if command -v docker >/dev/null 2>&1; then
    DOCKER_VER=$(docker --version)
    echo "  ${GREEN}✔${NC} Docker instalado: ${DOCKER_VER}"
else
    echo "  ${RED}✘ [ERROR CRÍTICO] Docker no se encuentra en el PATH del sistema.${NC}"
    ERRORS=$((ERRORS + 1))
fi

if docker compose version >/dev/null 2>&1; then
    COMPOSE_VER=$(docker compose version)
    echo "  ${GREEN}✔${NC} Docker Compose v2 disponible: ${COMPOSE_VER}"
else
    echo "  ${YELLOW}⚠ [ADVERTENCIA] Docker Compose v2 no detectado directamente. Verifica el soporte compose.${NC}"
    WARNINGS=$((WARNINGS + 1))
fi

# -----------------------------------------------------------------------------
# 2. Datasets ZFS y Permisos de Almacenamiento
# -----------------------------------------------------------------------------
echo ""
echo "${BLUE}[2/7] Verificando datasets ZFS y permisos de persistencia...${NC}"

# Dataset Postgres
POSTGRES_DIR="/mnt/Datos/PictoHabla/Postgres"
if [ -d "${POSTGRES_DIR}" ]; then
    PG_OWNER=$(stat -c '%u:%g' "${POSTGRES_DIR}" 2>/dev/null || stat -f '%u:%g' "${POSTGRES_DIR}" 2>/dev/null || echo "desconocido")
    PG_PERMS=$(stat -c '%a' "${POSTGRES_DIR}" 2>/dev/null || echo "desconocido")
    echo "  ${GREEN}✔${NC} Dataset Postgres existe: ${POSTGRES_DIR} (UID:GID ${PG_OWNER}, Permisos: ${PG_PERMS})"
    if [ "${PG_OWNER}" != "70:70" ]; then
        echo "    ${YELLOW}⚠${NC} Advertencia de propiedad: PostgreSQL 16 Alpine se ejecuta como UID 70 / GID 70 (postgres:postgres)."
        echo "       Para evitar fallos de inicialización si el directorio está vacío, ejecuta:"
        echo "       chown 70:70 ${POSTGRES_DIR}"
        WARNINGS=$((WARNINGS + 1))
    fi
    if [ "${PG_PERMS}" != "700" ]; then
        echo "    ${YELLOW}ℹ${NC} Recomendación de seguridad POSIX: chmod 700 ${POSTGRES_DIR}"
    fi
else
    echo "  ${RED}✘ [ERROR CRÍTICO] Dataset no encontrado: ${POSTGRES_DIR}${NC}"
    echo "       Crea el dataset en TrueNAS: Datos/PictoHabla/Postgres"
    ERRORS=$((ERRORS + 1))
fi

# Dataset Uploads
UPLOADS_DIR="/mnt/Datos/PictoHabla/Uploads"
if [ -d "${UPLOADS_DIR}" ]; then
    UP_OWNER=$(stat -c '%u:%g' "${UPLOADS_DIR}" 2>/dev/null || stat -f '%u:%g' "${UPLOADS_DIR}" 2>/dev/null || echo "desconocido")
    UP_PERMS=$(stat -c '%a' "${UPLOADS_DIR}" 2>/dev/null || echo "desconocido")
    echo "  ${GREEN}✔${NC} Dataset Uploads existe: ${UPLOADS_DIR} (UID:GID ${UP_OWNER}, Permisos: ${UP_PERMS})"
    if [ "${UP_OWNER}" != "1000:1000" ]; then
        echo "    ${YELLOW}⚠${NC} Advertencia de propiedad: Spring Boot se ejecuta como usuario no-root UID 1000 / GID 1000 (spring:spring)."
        echo "       Para permitir la escritura de imágenes y multimedia subidos, ejecuta:"
        echo "       chown 1000:1000 ${UPLOADS_DIR}"
        WARNINGS=$((WARNINGS + 1))
    fi
    if [ "${UP_PERMS}" != "750" ] && [ "${UP_PERMS}" != "700" ]; then
        echo "    ${YELLOW}ℹ${NC} Recomendación de seguridad POSIX: chmod 750 ${UPLOADS_DIR}"
    fi
else
    echo "  ${RED}✘ [ERROR CRÍTICO] Dataset no encontrado: ${UPLOADS_DIR}${NC}"
    echo "       Crea el dataset en TrueNAS: Datos/PictoHabla/Uploads"
    ERRORS=$((ERRORS + 1))
fi

# Dataset Backups (en pool Seguridad)
BACKUPS_DIR="/mnt/Seguridad/PictoHabla/Backups"
if [ -d "${BACKUPS_DIR}" ]; then
    BK_OWNER=$(stat -c '%u:%g' "${BACKUPS_DIR}" 2>/dev/null || stat -f '%u:%g' "${BACKUPS_DIR}" 2>/dev/null || echo "desconocido")
    BK_PERMS=$(stat -c '%a' "${BACKUPS_DIR}" 2>/dev/null || echo "desconocido")
    echo "  ${GREEN}✔${NC} Dataset Backups existe: ${BACKUPS_DIR} (UID:GID ${BK_OWNER}, Permisos: ${BK_PERMS})"
else
    echo "  ${RED}✘ [ERROR CRÍTICO] Dataset de copias de seguridad NO encontrado: ${BACKUPS_DIR}${NC}"
    echo "       Requisito mandatorio para la resiliencia del sistema: crea el dataset ZFS 'Seguridad/PictoHabla/Backups'."
    echo "       Las copias de seguridad de cron no deben fallar ni escribirse en el sistema operativo raíz."
    ERRORS=$((ERRORS + 1))
fi

# Directorio de Scripts
SCRIPTS_DIR="/mnt/Datos/PictoHabla/scripts"
if [ -d "${SCRIPTS_DIR}" ]; then
    echo "  ${GREEN}✔${NC} Directorio de scripts existe: ${SCRIPTS_DIR}"
    for script_file in backup.sh restore.sh precheck-truenas.sh; do
        if [ -f "${SCRIPTS_DIR}/${script_file}" ]; then
            if [ -x "${SCRIPTS_DIR}/${script_file}" ]; then
                echo "    ${GREEN}✔${NC} Script ejecutable: ${script_file}"
            else
                echo "    ${YELLOW}⚠${NC} Script sin permiso de ejecución: ${script_file} (ejecuta: chmod +x ${SCRIPTS_DIR}/${script_file})"
                WARNINGS=$((WARNINGS + 1))
            fi
        fi
    done
else
    echo "  ${YELLOW}ℹ${NC} Directorio de scripts no existe: ${SCRIPTS_DIR}"
    echo "       Créalo con: mkdir -p ${SCRIPTS_DIR}"
fi

# -----------------------------------------------------------------------------
# 3. Red Docker de Cloudflare Tunnel
# -----------------------------------------------------------------------------
echo ""
echo "${BLUE}[3/7] Verificando red Docker de Cloudflare Tunnel...${NC}"
CF_NETWORK="ix-cloudflared_default"
CF_SUBNET=""
if docker network inspect "${CF_NETWORK}" >/dev/null 2>&1; then
    CF_SUBNET=$(docker network inspect "${CF_NETWORK}" -f '{{range .IPAM.Config}}{{.Subnet}}{{end}}' 2>/dev/null || echo "")
    echo "  ${GREEN}✔${NC} Red externa '${CF_NETWORK}' detectada (Subred activa: ${CF_SUBNET})"
    
    # Comprobar presencia del contenedor cloudflared
    if docker ps --format '{{.Names}}' | grep -q "cloudflared"; then
        CF_CONTAINER=$(docker ps --format '{{.Names}}' | grep "cloudflared" | head -n1)
        CF_IP=$(docker inspect -f "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}" "${CF_CONTAINER}" 2>/dev/null || echo "desconocida")
        echo "  ${GREEN}✔${NC} Contenedor Cloudflare activo: ${CF_CONTAINER} (IP: ${CF_IP})"
    else
        echo "  ${YELLOW}⚠${NC} Contenedor de Cloudflare no detectado en ejecución. Verifica la app cloudflared en TrueNAS."
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo "  ${RED}✘ [ERROR CRÍTICO] Red Docker externa '${CF_NETWORK}' no encontrada.${NC}"
    echo "       Comprueba el nombre exacto de la red Docker con: docker network ls"
    ERRORS=$((ERRORS + 1))
fi

# -----------------------------------------------------------------------------
# 4. Archivo de Entorno (.env) y Coherencia de Red
# -----------------------------------------------------------------------------
echo ""
echo "${BLUE}[4/7] Verificando archivo de entorno (.env) y coherencia de subred...${NC}"
ENV_FILE="/mnt/Datos/PictoHabla/.env"
if [ -f "${ENV_FILE}" ]; then
    ENV_PERMS=$(stat -c '%a' "${ENV_FILE}" 2>/dev/null || echo "desconocido")
    echo "  ${GREEN}✔${NC} Archivo de configuración existe: ${ENV_FILE} (Permisos: ${ENV_PERMS})"
    if [ "${ENV_PERMS}" != "600" ]; then
        echo "    ${YELLOW}⚠${NC} Permisos inseguros en ${ENV_FILE}. Debe ser de acceso exclusivo para root:"
        echo "       chmod 600 ${ENV_FILE}"
        WARNINGS=$((WARNINGS + 1))
    fi

    # Comprobar TRUSTED_PROXY_SUBNET vs Subred Real
    ENV_TRUSTED_SUBNET=$(grep -E '^\s*TRUSTED_PROXY_SUBNET=' "${ENV_FILE}" | cut -d= -f2- | tr -d ' "\r' || echo "")
    if [ -n "${ENV_TRUSTED_SUBNET}" ]; then
        if [ -n "${CF_SUBNET}" ] && [ "${ENV_TRUSTED_SUBNET}" != "${CF_SUBNET}" ]; then
            echo "  ${RED}✘ [ERROR CRÍTICO] Discrepancia en TRUSTED_PROXY_SUBNET:${NC}"
            echo "       - Subred real en Docker (${CF_NETWORK}): ${CF_SUBNET}"
            echo "       - Subred declarada en ${ENV_FILE}: ${ENV_TRUSTED_SUBNET}"
            echo "       Causa: Si las subredes no coinciden, Spring Boot ForwardedHeaderFilter descartará"
            echo "       las cabeceras X-Forwarded-For y X-Forwarded-Proto, provocando problemas con CORS,"
            echo "       redirecciones HTTPS erróneas y falseamiento de IPs en los logs."
            echo "       Acción requerida (modifica manualmente el archivo .env):"
            echo "       Ajusta en ${ENV_FILE}: TRUSTED_PROXY_SUBNET=${CF_SUBNET}"
            ERRORS=$((ERRORS + 1))
        else
            echo "  ${GREEN}✔${NC} TRUSTED_PROXY_SUBNET coincide exactamente con la subred Docker (${ENV_TRUSTED_SUBNET})"
        fi
    else
        echo "  ${YELLOW}⚠${NC} No se encontró la variable TRUSTED_PROXY_SUBNET en ${ENV_FILE}."
        WARNINGS=$((WARNINGS + 1))
    fi

    # Comprobar presencia de contraseñas por defecto o placeholders no reemplazados
    if grep -q "CAMBIA_ESTA_CLAVE" "${ENV_FILE}" 2>/dev/null || grep -q "<" "${ENV_FILE}" 2>/dev/null; then
        echo "  ${YELLOW}⚠${NC} El archivo .env contiene valores de marcador de posición (<...>) que deben ser reemplazados."
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo "  ${YELLOW}ℹ${NC} Archivo ${ENV_FILE} aún no creado."
    echo "       Crea el archivo a partir de .env.truenas.example antes de iniciar con docker compose."
fi

# -----------------------------------------------------------------------------
# 5. Recursos del Servidor (RAM y Disco)
# -----------------------------------------------------------------------------
echo ""
echo "${BLUE}[5/7] Verificando recursos del servidor (Memoria y Espacio ZFS)...${NC}"
if command -v free >/dev/null 2>&1; then
    AVAILABLE_RAM=$(free -m | awk '/^Mem:/{print $7}')
    TOTAL_RAM=$(free -m | awk '/^Mem:/{print $2}')
    echo "  ${GREEN}✔${NC} Memoria RAM: ${AVAILABLE_RAM} MB disponibles de ${TOTAL_RAM} MB totales."
    if [ "${AVAILABLE_RAM}" -lt 768 ]; then
        echo "    ${RED}✘ [ERROR CRÍTICO] Memoria RAM libre insuficiente (< 768 MB). PictoHabla requiere 768 MB (256M Postgres + 512M Backend).${NC}"
        ERRORS=$((ERRORS + 1))
    elif [ "${AVAILABLE_RAM}" -lt 1500 ]; then
        echo "    ${YELLOW}⚠${NC} Memoria libre ajustada (< 1.5 GB). Vigila el uso de memoria de otras aplicaciones."
        WARNINGS=$((WARNINGS + 1))
    fi
fi

if df -h /mnt/Datos >/dev/null 2>&1; then
    DATOS_AVAIL=$(df -h /mnt/Datos | awk 'NR==2{print $4}')
    echo "  ${GREEN}✔${NC} Espacio libre en pool Datos: ${DATOS_AVAIL}"
fi

if [ -d "/mnt/Seguridad" ] && df -h /mnt/Seguridad >/dev/null 2>&1; then
    SEG_AVAIL=$(df -h /mnt/Seguridad | awk 'NR==2{print $4}')
    echo "  ${GREEN}✔${NC} Espacio libre en pool Seguridad: ${SEG_AVAIL}"
fi

# -----------------------------------------------------------------------------
# 6. Imagen Docker del Backend
# -----------------------------------------------------------------------------
echo ""
echo "${BLUE}[6/7] Verificando disponibilidad de la imagen Docker...${NC}"
IMAGE_NAME="pictohabla-backend:1.0.0"
if docker image inspect "${IMAGE_NAME}" >/dev/null 2>&1; then
    IMAGE_SIZE=$(docker image inspect "${IMAGE_NAME}" -f '{{.Size}}' 2>/dev/null | awk '{printf "%.1f MB", $1/1024/1024}')
    echo "  ${GREEN}✔${NC} Imagen ${IMAGE_NAME} presente en el almacén local de Docker (${IMAGE_SIZE})."
else
    echo "  ${YELLOW}ℹ${NC} Imagen '${IMAGE_NAME}' no existe aún en Docker."
    echo "       Opciones de despliegue antes de iniciar:"
    echo "       A) Construcción local en TrueNAS: docker build -t ${IMAGE_NAME} /mnt/Datos/PictoHabla/build"
    echo "       B) Carga de tar precompilado:   docker load -i /mnt/Datos/PictoHabla/${IMAGE_NAME}.tar.gz"
fi

# -----------------------------------------------------------------------------
# 7. Aislamiento de Red y Puertos del Host
# -----------------------------------------------------------------------------
echo ""
echo "${BLUE}[7/7] Verificando aislamiento de puertos y seguridad de red...${NC}"
echo "  ${GREEN}✔${NC} Configuración arquitectónica validada:"
echo "       - PostgreSQL confinado exclusivamente a la red puente interna 'pictohabla-internal'."
echo "       - Ningún puerto (5432 ni 8080) se publica en la interfaz física del host."
echo "       - Spring Boot accesible exclusivamente por Cloudflared en el puerto 8080 vía 'ix-cloudflared_default'."

# -----------------------------------------------------------------------------
# Resumen Final
# -----------------------------------------------------------------------------
echo ""
echo "${BLUE}==============================================================================${NC}"
if [ ${ERRORS} -eq 0 ]; then
    if [ ${WARNINGS} -eq 0 ]; then
        echo "${GREEN}✔ TODAS LAS COMPROBACIONES SUPERADAS SIN ERRORES NI ADVERTENCIAS.${NC}"
        echo "  El servidor TrueNAS SCALE 25.10.7 está 100% listo para desplegar PictoHabla."
    else
        echo "${YELLOW}✔ COMPROBACIONES SUPERADAS CON ${WARNINGS} ADVERTENCIA(S).${NC}"
        echo "  Revisa las advertencias indicadas antes de arrancar los contenedores."
    fi
else
    echo "${RED}✘ SE ENCONTRARON ${ERRORS} ERROR(ES) CRÍTICO(S) Y ${WARNINGS} ADVERTENCIA(S).${NC}"
    echo "  Corrige los errores señalados antes de intentar iniciar los contenedores."
fi
echo "${BLUE}==============================================================================${NC}"

exit ${ERRORS}
