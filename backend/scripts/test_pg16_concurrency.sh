#!/bin/bash
set -e

service docker start >/dev/null 2>&1 || true
docker rm -f pg16-concur >/dev/null 2>&1 || true
docker run -d --name pg16-concur -e POSTGRES_PASSWORD=secret postgres:16-alpine
sleep 3
docker exec pg16-concur pg_isready

# Aplicar migraciones V1, V2 y V3
docker cp /mnt/c/Users/carlo/AndroidStudioProjects/PictoHabla/backend/src/main/resources/db/migration/V1__initial_schema.sql pg16-concur:/tmp/V1.sql
docker cp /mnt/c/Users/carlo/AndroidStudioProjects/PictoHabla/backend/src/main/resources/db/migration/V2__add_teacher_role.sql pg16-concur:/tmp/V2.sql
docker cp /mnt/c/Users/carlo/AndroidStudioProjects/PictoHabla/backend/src/main/resources/db/migration/V3__admin_bootstrap_protection.sql pg16-concur:/tmp/V3.sql

docker exec -i pg16-concur psql -U postgres -d postgres -f /tmp/V1.sql >/dev/null
docker exec -i pg16-concur psql -U postgres -d postgres -f /tmp/V2.sql >/dev/null
docker exec -i pg16-concur psql -U postgres -d postgres -f /tmp/V3.sql >/dev/null

echo "=== MIGRACIONES V1, V2, V3 CARGADAS EN POSTGRESQL 16 ==="

# Script para Sesión 1: Adquiere bloqueo, duerme 3s, inserta admin1 y metadata, hace commit
cat << 'EOF' > /tmp/session1.sql
BEGIN;
SELECT pg_advisory_xact_lock(7821948201948201);
SELECT pg_sleep(3);
INSERT INTO teachers (id, email, password_hash, full_name, status, role, email_verified, created_at, updated_at)
VALUES ('11111111-aaaa-bbbb-cccc-111111111111', 'admin1@colegio.es', 'hash1', 'Admin Uno', 'ACTIVE', 'ADMIN', true, NOW(), NOW());
INSERT INTO admin_bootstrap_metadata (id, completed, completed_at, admin_email)
VALUES (1, TRUE, NOW(), 'admin1@colegio.es');
COMMIT;
SELECT 'SESION 1 COMPLETADA CON EXITO' as resultado;
EOF

# Script para Sesión 2: Intenta adquirir bloqueo (se bloqueará hasta que Sesión 1 haga commit),
# al despertar comprueba si admin_bootstrap_metadata ya existe, e intenta insertar admin2.
cat << 'EOF' > /tmp/session2.sql
BEGIN;
SELECT pg_advisory_xact_lock(7821948201948201);
DO $$
DECLARE
    v_count INT;
BEGIN
    SELECT COUNT(*) INTO v_count FROM admin_bootstrap_metadata;
    IF v_count > 0 THEN
        RAISE EXCEPTION 'BOOTSTRAP_YA_COMPLETADO: El bootstrap ya fue completado previamente por otra sesion.';
    END IF;
    INSERT INTO teachers (id, email, password_hash, full_name, status, role, email_verified, created_at, updated_at)
    VALUES ('22222222-aaaa-bbbb-cccc-222222222222', 'admin2@colegio.es', 'hash2', 'Admin Dos', 'ACTIVE', 'ADMIN', true, NOW(), NOW());
    INSERT INTO admin_bootstrap_metadata (id, completed, completed_at, admin_email)
    VALUES (1, TRUE, NOW(), 'admin2@colegio.es');
END $$;
COMMIT;
SELECT 'SESION 2 COMPLETADA CON EXITO' as resultado;
EOF

docker cp /tmp/session1.sql pg16-concur:/tmp/session1.sql
docker cp /tmp/session2.sql pg16-concur:/tmp/session2.sql

echo "=== LANZANDO SESION 1 Y SESION 2 EN PARALELO CONTRA POSTGRESQL 16 ==="
docker exec -i pg16-concur psql -U postgres -d postgres -f /tmp/session1.sql > /tmp/s1.log 2>&1 &
PID1=$!

sleep 0.5
docker exec -i pg16-concur psql -U postgres -d postgres -f /tmp/session2.sql > /tmp/s2.log 2>&1 &
PID2=$!

wait $PID1
wait $PID2 || true

echo "=== RESULTADO SESION 1 ==="
cat /tmp/s1.log

echo "=== RESULTADO SESION 2 ==="
cat /tmp/s2.log

echo "=== CONTEO FINAL EN POSTGRESQL 16 ==="
docker exec -i pg16-concur psql -U postgres -d postgres -c "
SELECT COUNT(*) as admins_en_teachers FROM teachers WHERE role = 'ADMIN';
SELECT COUNT(*) as registros_metadata FROM admin_bootstrap_metadata;
SELECT email, role, admin_singleton FROM teachers WHERE role = 'ADMIN';
"

docker rm -f pg16-concur >/dev/null 2>&1 || true
echo "=== PRUEBA DE CONCURRENCIA CON DOS SESIONES INDEPENDIENTES EN POSTGRESQL 16 COMPLETADA ==="
