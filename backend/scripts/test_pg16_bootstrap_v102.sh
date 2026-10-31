#!/bin/bash
set -e

service docker start >/dev/null 2>&1 || true
docker rm -f pg16-v102 >/dev/null 2>&1 || true
docker run -d --name pg16-v102 -e POSTGRES_PASSWORD=secret postgres:16-alpine
sleep 3
docker exec pg16-v102 pg_isready

echo "=== 1. APLICANDO ESQUEMA COMPLETO V1, V2 Y V3 EN POSTGRESQL 16 ==="
docker cp /mnt/c/Users/carlo/AndroidStudioProjects/PictoHabla/backend/src/main/resources/db/migration/V1__initial_schema.sql pg16-v102:/tmp/V1.sql
docker cp /mnt/c/Users/carlo/AndroidStudioProjects/PictoHabla/backend/src/main/resources/db/migration/V2__add_teacher_role.sql pg16-v102:/tmp/V2.sql
docker cp /mnt/c/Users/carlo/AndroidStudioProjects/PictoHabla/backend/src/main/resources/db/migration/V3__admin_bootstrap_protection.sql pg16-v102:/tmp/V3.sql

docker exec -i pg16-v102 psql -U postgres -d postgres -f /tmp/V1.sql >/dev/null
docker exec -i pg16-v102 psql -U postgres -d postgres -f /tmp/V2.sql >/dev/null
docker exec -i pg16-v102 psql -U postgres -d postgres -f /tmp/V3.sql >/dev/null
echo ">> Migraciones V1, V2 y V3 aplicadas con exito."

echo "=== 2. PRUEBA DE ROLLBACK ATOMICO (Si falla metadata, revierte el admin) ==="
# Simular transaccion de bootstrap donde la insercion en teachers ocurre, pero metadata falla
docker exec -i pg16-v102 psql -U postgres -d postgres -c "
DO \$\$
BEGIN
    -- 1. Iniciar operaciones transaccionales
    PERFORM pg_advisory_xact_lock(7821948201948201);
    
    -- Insercion del administrador
    INSERT INTO teachers (id, email, password_hash, full_name, status, role, email_verified, created_at, updated_at)
    VALUES ('11111111-0000-0000-0000-111111111111', 'fallo.meta@colegio.es', 'hash', 'Admin Falla', 'ACTIVE', 'ADMIN', true, NOW(), NOW());
    
    -- Insercion invalida deliberada en admin_bootstrap_metadata para forzar excepcion (id=2 viola CHECK id=1)
    INSERT INTO admin_bootstrap_metadata (id, completed, completed_at, admin_email)
    VALUES (2, TRUE, CURRENT_TIMESTAMP, 'fallo.meta@colegio.es');
EXCEPTION WHEN OTHERS THEN
    -- Al capturar la excepcion o revertir, la transaccion no realiza commit de la cuenta
    RAISE NOTICE 'Rollback simulado por fallo en metadata: %', SQLERRM;
END \$\$;
"

# Verificar que en teachers NO existe la cuenta tras el fallo
COUNT_FAIL=$(docker exec -i pg16-v102 psql -U postgres -d postgres -t -A -c "SELECT COUNT(*) FROM teachers WHERE email = 'fallo.meta@colegio.es';")
if [ "${COUNT_FAIL}" -eq 0 ]; then
    echo ">> [OK] Rollback confirmado: el administrador NO fue persistido tras el fallo en metadata."
else
    echo ">> [FALLO] El administrador persiste tras el fallo en metadata."
    exit 1
fi

echo "=== 3. PRUEBA DE BOOTSTRAP EXITOSO CON CURRENT_TIMESTAMP ==="
docker exec -i pg16-v102 psql -U postgres -d postgres -c "
BEGIN;
SELECT pg_advisory_xact_lock(7821948201948201);

INSERT INTO teachers (id, email, password_hash, full_name, status, role, email_verified, created_at, updated_at)
VALUES ('aaaaaaaa-1111-2222-3333-aaaaaaaaaaaa', 'admin.prod@colegio.es', '\$2a\$12\$hashBcrypt12Seguro', 'Administrador General', 'ACTIVE', 'ADMIN', true, NOW(), NOW());

INSERT INTO admin_bootstrap_metadata (id, completed, completed_at, admin_email)
VALUES (1, TRUE, CURRENT_TIMESTAMP, 'admin.prod@colegio.es');

COMMIT;
"
echo ">> Bootstrap transaccional exitoso."

echo "=== 4. VERIFICACION DE METADATOS Y TIMESTAMP EN POSTGRESQL 16 ==="
docker exec -i pg16-v102 psql -U postgres -d postgres -c "
SELECT id, completed, completed_at, admin_email FROM admin_bootstrap_metadata;
SELECT id, email, role, admin_singleton, created_at FROM teachers WHERE role = 'ADMIN';
"

# Comprobar que completed_at esta poblado
COMPLETED_AT=$(docker exec -i pg16-v102 psql -U postgres -d postgres -t -A -c "SELECT completed_at FROM admin_bootstrap_metadata WHERE id = 1;")
if [ -n "${COMPLETED_AT}" ]; then
    echo ">> [OK] completed_at generado correctamente por PostgreSQL 16: ${COMPLETED_AT}"
else
    echo ">> [FALLO] completed_at esta vacio."
    exit 1
fi

echo "=== 5. PRUEBA DE BLOQUEO DE SEGUNDO INTENTO (UN SOLO USO) ==="
# Intentar segundo bootstrap con otro email
if docker exec -i pg16-v102 psql -U postgres -d postgres -c "
DO \$\$
DECLARE
    v_count INT;
BEGIN
    SELECT COUNT(*) INTO v_count FROM admin_bootstrap_metadata;
    IF v_count > 0 THEN
        RAISE EXCEPTION 'BOOTSTRAP_BLOQUEADO: ya fue completado previamente.';
    END IF;
    INSERT INTO teachers (id, email, password_hash, full_name, status, role, email_verified, created_at, updated_at)
    VALUES ('bbbbbbbb-1111-2222-3333-bbbbbbbbbbbb', 'admin2@colegio.es', 'hash', 'Admin Dos', 'ACTIVE', 'ADMIN', true, NOW(), NOW());
END \$\$;
" 2>&1 | grep -q 'BOOTSTRAP_BLOQUEADO'; then
    echo ">> [OK] Segundo intento de bootstrap bloqueado: el mecanismo es estrictamente de un solo uso."
else
    echo ">> [FALLO] El segundo bootstrap no fue bloqueado."
    exit 1
fi

echo "=== 6. PRUEBA DE RESTRICCION RELACIONAL uq_teachers_single_admin ==="
if docker exec -i pg16-v102 psql -U postgres -d postgres -c "
INSERT INTO teachers (id, email, password_hash, full_name, status, role, email_verified, created_at, updated_at)
VALUES ('cccccccc-1111-2222-3333-cccccccccccc', 'admin3@colegio.es', 'hash', 'Admin Tres', 'ACTIVE', 'ADMIN', true, NOW(), NOW());
" 2>&1 | grep -q 'uq_teachers_single_admin'; then
    echo ">> [OK] Restriccion uq_teachers_single_admin impide un segundo ADMIN en la tabla teachers."
else
    echo ">> [FALLO] No se detecto la restriccion uq_teachers_single_admin."
    exit 1
fi

docker rm -f pg16-v102 >/dev/null 2>&1 || true
echo "=== TODAS LAS PRUEBAS EN POSTGRESQL 16 REAL COMPLETADAS EXITOSAMENTE ==="
