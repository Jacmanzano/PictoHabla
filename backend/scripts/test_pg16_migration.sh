#!/bin/bash
set -e

service docker start >/dev/null 2>&1 || true
docker rm -f pg16-audit >/dev/null 2>&1 || true
docker run -d --name pg16-audit -e POSTGRES_PASSWORD=secret postgres:16-alpine
sleep 3
docker exec pg16-audit pg_isready

echo "=== 1. APLICANDO V1 ==="
docker cp /mnt/c/Users/carlo/AndroidStudioProjects/PictoHabla/backend/src/main/resources/db/migration/V1__initial_schema.sql pg16-audit:/tmp/V1.sql
docker exec -i pg16-audit psql -U postgres -d postgres -f /tmp/V1.sql

echo "=== 2. INSERTANDO DATOS EXISTENTES (Simulando producción antes de V2) ==="
docker exec -i pg16-audit psql -U postgres -d postgres -c "
INSERT INTO teachers (id, email, password_hash, full_name, status, email_verified, created_at, updated_at)
VALUES 
  ('11111111-1111-1111-1111-111111111111', 'profe1@colegio.es', 'hash1', 'Profesor Uno', 'ACTIVE', true, NOW(), NOW()),
  ('22222222-2222-2222-2222-222222222222', 'profe2@colegio.es', 'hash2', 'Profesor Dos', 'ACTIVE', true, NOW(), NOW());
"

echo "=== 3. APLICANDO V2 ==="
docker cp /mnt/c/Users/carlo/AndroidStudioProjects/PictoHabla/backend/src/main/resources/db/migration/V2__add_teacher_role.sql pg16-audit:/tmp/V2.sql
docker exec -i pg16-audit psql -U postgres -d postgres -f /tmp/V2.sql

echo "=== 4. VERIFICANDO ROLES TRAS V2 ==="
docker exec -i pg16-audit psql -U postgres -d postgres -c "SELECT id, email, role FROM teachers;"

echo "=== 5. APLICANDO V3 CON STORED ==="
docker cp /mnt/c/Users/carlo/AndroidStudioProjects/PictoHabla/backend/src/main/resources/db/migration/V3__admin_bootstrap_protection.sql pg16-audit:/tmp/V3.sql
docker exec -i pg16-audit psql -U postgres -d postgres -f /tmp/V3.sql

echo "=== 6. VERIFICANDO COLUMNA GENERADA admin_singleton EN DATOS EXISTENTES ==="
docker exec -i pg16-audit psql -U postgres -d postgres -c "SELECT id, email, role, admin_singleton FROM teachers;"

echo "=== 7. PRUEBA: INSERTAR MÚLTIPLES PROFESORES (NULL en admin_singleton) ==="
docker exec -i pg16-audit psql -U postgres -d postgres -c "
INSERT INTO teachers (id, email, password_hash, full_name, status, role, email_verified, created_at, updated_at)
VALUES 
  ('33333333-3333-3333-3333-333333333333', 'profe3@colegio.es', 'hash3', 'Profesor Tres', 'ACTIVE', 'TEACHER', true, NOW(), NOW()),
  ('44444444-4444-4444-4444-444444444444', 'profe4@colegio.es', 'hash4', 'Profesor Cuatro', 'ACTIVE', 'TEACHER', true, NOW(), NOW());
"
echo ">> Inserción de múltiples TEACHER exitosa (NULL en admin_singleton no colisiona en UNIQUE)."

echo "=== 8. PRUEBA: INSERTAR EL PRIMER ADMIN ==="
docker exec -i pg16-audit psql -U postgres -d postgres -c "
INSERT INTO teachers (id, email, password_hash, full_name, status, role, email_verified, created_at, updated_at)
VALUES 
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'admin1@colegio.es', 'hash_admin', 'Admin Primero', 'ACTIVE', 'ADMIN', true, NOW(), NOW());
"
echo ">> Inserción de primer ADMIN exitosa (admin_singleton = 'ADMIN')."

echo "=== 9. PRUEBA: INTENTO DE INSERTAR SEGUNDO ADMIN (Debe fallar por uq_teachers_single_admin) ==="
if docker exec -i pg16-audit psql -U postgres -d postgres -c "
INSERT INTO teachers (id, email, password_hash, full_name, status, role, email_verified, created_at, updated_at)
VALUES 
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'admin2@colegio.es', 'hash_admin2', 'Admin Segundo', 'ACTIVE', 'ADMIN', true, NOW(), NOW());
" 2>&1 | grep -q 'uq_teachers_single_admin'; then
    echo ">> [OK] Segundo ADMIN bloqueado por la restricción UNIQUE uq_teachers_single_admin."
else
    echo ">> [FALLO] No se detectó la violación esperada uq_teachers_single_admin."
    exit 1
fi

echo "=== 10. PRUEBA: admin_bootstrap_metadata (Primer Registro) ==="
docker exec -i pg16-audit psql -U postgres -d postgres -c "
INSERT INTO admin_bootstrap_metadata (id, completed, completed_at, admin_email)
VALUES (1, TRUE, NOW(), 'admin1@colegio.es');
"
echo ">> Primer registro en admin_bootstrap_metadata exitoso."

echo "=== 11. PRUEBA: DUPLICAR id=1 EN admin_bootstrap_metadata (Debe fallar por PK) ==="
if docker exec -i pg16-audit psql -U postgres -d postgres -c "
INSERT INTO admin_bootstrap_metadata (id, completed, completed_at, admin_email)
VALUES (1, TRUE, NOW(), 'otro@colegio.es');
" 2>&1 | grep -q 'admin_bootstrap_metadata_pkey'; then
    echo ">> [OK] Duplicado de id=1 bloqueado por Primary Key."
else
    echo ">> [FALLO] No se detectó la violación de Primary Key en admin_bootstrap_metadata."
    exit 1
fi

echo "=== 12. PRUEBA: INSERTAR id=2 EN admin_bootstrap_metadata (Debe fallar por CHECK id=1) ==="
if docker exec -i pg16-audit psql -U postgres -d postgres -c "
INSERT INTO admin_bootstrap_metadata (id, completed, completed_at, admin_email)
VALUES (2, TRUE, NOW(), 'otro@colegio.es');
" 2>&1 | grep -q 'chk_single_bootstrap_record'; then
    echo ">> [OK] Registro id=2 bloqueado por CHECK constraint (chk_single_bootstrap_record)."
else
    echo ">> [FALLO] No se detectó la violación de CHECK constraint en admin_bootstrap_metadata."
    exit 1
fi

docker rm -f pg16-audit >/dev/null 2>&1 || true
echo "=== TODAS LAS PRUEBAS DE MIGRACIÓN EN POSTGRESQL 16 COMPLETADAS CON ÉXITO ==="
