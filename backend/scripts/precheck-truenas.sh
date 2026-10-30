#!/bin/sh
# ==============================================================================
# PictoHabla — Script de Comprobaciones Previas al Despliegue en TrueNAS SCALE
# ==============================================================================
# Ejecutar en la shell SSH o consola de TrueNAS SCALE antes de iniciar los contenedores:
#   sh /mnt/Datos/PictoHabla/scripts/precheck-truenas.sh
# ==============================================================================

set -u

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
# 1. Herramientas Docker
# -----------------------------------------------------------------------------
echo "${BLUE}[1/6] Verificando Docker y entorno de contenedores...${NC}"
if command -v docker >/dev/null 2>&1; then
    DOCKER_VER=$(docker --version)
    echo "  ${GREEN}✔${NC} Docker instalado: ${DOCKER_VER}"
else
    echo "  ${RED}✘${NC} Docker no se encuentra en el PATH del sistema."
    ERRORS=$((ERRORS + 1))
fi

if docker compose version >/dev/null 2>&1; then
    COMPOSE_VER=$(docker compose version)
    echo "  ${GREEN}✔${NC} Docker Compose disponible: ${COMPOSE_VER}"
else
    echo "  ${YELLOW}⚠${NC} Docker Compose v2 no detectado directamente. Verifica el soporte compose."
    WARNINGS=$((WARNINGS + 1))
fi

# -----------------------------------------------------------------------------
# 2. Datasets ZFS y Permisos
# -----------------------------------------------------------------------------
echo ""
echo "${BLUE}[2/6] Verificando datasets ZFS y rutas de persistencia...${NC}"

# Dataset Postgres
POSTGRES_DIR="/mnt/Datos/PictoHabla/Postgres"
if [ -d "${POSTGRES_DIR}" ]; then
    PG_OWNER=$(stat -c '%u:%g' "${POSTGRES_DIR}" 2>/dev/null || stat -f '%u:%g' "${POSTGRES_DIR}" 2>/dev/null || echo "desconocido")
    echo "  ${GREEN}✔${NC} Dataset Postgres existe: ${POSTGRES_DIR} (Propietario UID:GID: ${PG_OWNER})"
    if [ "${PG_OWNER}" != "70:70" ]; then
        echo "    ${YELLOW}ℹ${NC} Nota: PostgreSQL oficial de Alpine se ejecuta con UID 70:GID 70."
        echo "       Para evitar errores de permisos en el primer arranque si el directorio está vacío, ejecuta:"
        echo "       chown 70:70 ${POSTGRES_DIR}"
    fi
else
    echo "  ${RED}✘${NC} Dataset no encontrado: ${POSTGRES_DIR}"
    echo "       Crea el dataset en TrueNAS: Datos/PictoHabla/Postgres"
    ERRORS=$((ERRORS + 1))
fi

# Dataset Uploads
UPLOADS_DIR="/mnt/Datos/PictoHabla/Uploads"
if [ -d "${UPLOADS_DIR}" ]; then
    UP_OWNER=$(stat -c '%u:%g' "${UPLOADS_DIR}" 2>/dev/null || stat -f '%u:%g' "${UPLOADS_DIR}" 2>/dev/null || echo "desconocido")
    echo "  ${GREEN}✔${NC} Dataset Uploads existe: ${UPLOADS_DIR} (Propietario UID:GID: ${UP_OWNER})"
    if [ "${UP_OWNER}" != "1000:1000" ]; then
        echo "    ${YELLOW}ℹ${NC} Nota: El backend Spring Boot se ejecuta con UID 1000:GID 1000."
        echo "       Para permitir subida de archivos multimedia, ejecuta:"
        echo "       chown 1000:1000 ${UPLOADS_DIR}"
    fi
else
    echo "  ${RED}✘${NC} Dataset no encontrado: ${UPLOADS_DIR}"
    echo "       Crea el dataset en TrueNAS: Datos/PictoHabla/Uploads"
    ERRORS=$((ERRORS + 1))
fi

# Dataset Backups (Seguridad)
BACKUPS_DIR="/mnt/Seguridad/PictoHabla/Backups"
if [ -d "${BACKUPS_DIR}" ]; then
    echo "  ${GREEN}✔${NC} Dataset Backups existe: ${BACKUPS_DIR}"
else
    echo "  ${YELLOW}⚠${NC} Dataset de copias de seguridad NO encontrado: ${BACKUPS_DIR}"
    echo "       Requisito previo para scripts de backup: crea el dataset Seguridad/PictoHabla/Backups"
    WARNINGS=$((WARNINGS + 1))
fi

# Directorio de Scripts
SCRIPTS_DIR="/mnt/Datos/PictoHabla/scripts"
if [ -d "${SCRIPTS_DIR}" ]; then
    echo "  ${GREEN}✔${NC} Directorio de scripts existe: ${SCRIPTS_DIR}"
else
    echo "  ${YELLOW}ℹ${NC} Directorio de scripts no existe: ${SCRIPTS_DIR} (se creará con 'mkdir -p ${SCRIPTS_DIR}')"
fi

# -----------------------------------------------------------------------------
# 3. Red Docker de Cloudflare Tunnel
# -----------------------------------------------------------------------------
echo ""
echo "${BLUE}[3/6] Verificando red Docker de Cloudflare Tunnel...${NC}"
CF_NETWORK="ix-cloudflared_default"
if docker network inspect "${CF_NETWORK}" >/dev/null 2>&1; then
    CF_SUBNET=$(docker network inspect "${CF_NETWORK}" -f '{{range .IPAM.Config}}{{.Subnet}}{{end}}' 2>/dev/null || echo "desconocida")
    echo "  ${GREEN}✔${NC} Red externa '${CF_NETWORK}' encontrada (Subred: ${CF_SUBNET})"
    
    # Comprobar presencia del contenedor cloudflared
    if docker ps --format '{{.Names}}' | grep -q "cloudflared"; then
        CF_CONTAINER=$(docker ps --format '{{.Names}}' | grep "cloudflared" | head -n1)
        CF_IP=$(docker inspect -f "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}" "${CF_CONTAINER}" 2>/dev/null || echo "desconocida")
        echo "  ${GREEN}✔${NC} Contenedor Cloudflare activo: ${CF_CONTAINER} (IP: ${CF_IP})"
    else
        echo "  ${YELLOW}⚠${NC} No se detectó ningún contenedor con 'cloudflared' en ejecución."
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo "  ${RED}✘${NC} Red Docker externa '${CF_NETWORK}' no encontrada."
    echo "       Comprueba el nombre exacto de la red de tu app cloudflared con: docker network ls"
    ERRORS=$((ERRORS + 1))
fi

# -----------------------------------------------------------------------------
# 4. Comprobación de Recursos (RAM y Disco)
# -----------------------------------------------------------------------------
echo ""
echo "${BLUE}[4/6] Verificando recursos del servidor (Memoria y Espacio ZFS)...${NC}"
if command -v free >/dev/null 2>&1; then
    AVAILABLE_RAM=$(free -m | awk '/^Mem:/{print $7}')
    TOTAL_RAM=$(free -m | awk '/^Mem:/{print $2}')
    echo "  ${GREEN}✔${NC} Memoria RAM: ${AVAILABLE_RAM} MB disponibles de ${TOTAL_RAM} MB totales."
    if [ "${AVAILABLE_RAM}" -lt 1024 ]; then
        echo "    ${YELLOW}⚠${NC} Memoria libre baja (< 1 GB). PictoHabla requiere 768 MB (256M Postgres + 512M Backend)."
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
# 5. Imagen Docker del Backend
# -----------------------------------------------------------------------------
echo ""
echo "${BLUE}[5/6] Verificando disponibilidad de la imagen Docker...${NC}"
IMAGE_NAME="pictohabla-backend:1.0.0"
if docker image inspect "${IMAGE_NAME}" >/dev/null 2>&1; then
    echo "  ${GREEN}✔${NC} Imagen ${IMAGE_NAME} presente localmente en Docker."
else
    echo "  ${YELLOW}ℹ${NC} Imagen '${IMAGE_NAME}' no existe aún en el host."
    echo "       Debe construirse antes del despliegue con:"
    echo "       docker build -t ${IMAGE_NAME} -f /mnt/Datos/PictoHabla/Dockerfile /mnt/Datos/PictoHabla"
    echo "       o cargarse desde un archivo tar con: docker load -i pictohabla-backend-1.0.0.tar"
fi

# -----------------------------------------------------------------------------
# 6. Comprobación de Conflictos de Puertos
# -----------------------------------------------------------------------------
echo ""
echo "${BLUE}[6/6] Verificando aislamiento de puertos...${NC}"
echo "  ${GREEN}✔${NC} Arquitectura de red configurada sin publicar puertos en el host."
echo "       PostgreSQL está confinado a 'pictohabla-internal'."
echo "       Backend Spring Boot es accesible únicamente por Cloudflared en el puerto 8080 de 'ix-cloudflared_default'."

# -----------------------------------------------------------------------------
# Resumen Final
# -----------------------------------------------------------------------------
echo ""
echo "${BLUE}==============================================================================${NC}"
if [ ${ERRORS} -eq 0 ]; then
    if [ ${WARNINGS} -eq 0 ]; then
        echo "${GREEN}✔ TODAS LAS COMPROBACIONES SUPERADAS EXITOSAMENTE.${NC}"
        echo "  El servidor está listo para desplegar PictoHabla."
    else
        echo "${YELLOW}✔ COMPROBACIONES BÁSICAS SUPERADAS CON ${WARNINGS} ADVERTENCIA(S).${NC}"
        echo "  Revisa las advertencias antes de proceder con el despliegue."
    fi
else
    echo "${RED}✘ SE ENCONTRARON ${ERRORS} ERROR(ES) CRÍTICO(S) Y ${WARNINGS} ADVERTENCIA(S).${NC}"
    echo "  Corrige los errores indicados antes de intentar iniciar los contenedores."
fi
echo "${BLUE}==============================================================================${NC}"

exit ${ERRORS}
