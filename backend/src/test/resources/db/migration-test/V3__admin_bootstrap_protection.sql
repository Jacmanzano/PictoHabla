-- ========================================================
-- PictoHabla CAA - Migración V3: Protección e Inmutabilidad de Bootstrap (H2 Test Profile)
-- En H2, la sintaxis GENERATED ALWAYS AS (...) no admite el token STORED.
-- En PostgreSQL 16 (producción), STORED es obligatorio y se incluye en src/main/resources/db/migration/V3.
-- ========================================================

-- 1. Tabla de metadatos persistentes de bootstrap de administrador
CREATE TABLE admin_bootstrap_metadata (
    id INT DEFAULT 1 PRIMARY KEY,
    completed BOOLEAN NOT NULL DEFAULT TRUE,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    admin_email VARCHAR(255) NOT NULL,
    CONSTRAINT chk_single_bootstrap_record CHECK (id = 1)
);

-- 2. Columna generada y restricción UNIQUE que impide más de un ADMIN a nivel de base de datos
ALTER TABLE teachers ADD COLUMN admin_singleton VARCHAR(10) GENERATED ALWAYS AS (CASE WHEN role = 'ADMIN' THEN 'ADMIN' ELSE NULL END);
ALTER TABLE teachers ADD CONSTRAINT uq_teachers_single_admin UNIQUE (admin_singleton);
