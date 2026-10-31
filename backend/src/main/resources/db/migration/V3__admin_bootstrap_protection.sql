-- ========================================================
-- PictoHabla CAA - Migración V3: Protección e Inmutabilidad de Bootstrap
-- ========================================================

-- 1. Tabla de metadatos persistentes de bootstrap de administrador
-- Garantiza a nivel de esquema relacional que el bootstrap solo puede completarse una vez.
-- La restricción CHECK (id = 1) y PRIMARY KEY impiden estrictamente más de una fila.
CREATE TABLE admin_bootstrap_metadata (
    id INT DEFAULT 1 PRIMARY KEY,
    completed BOOLEAN NOT NULL DEFAULT TRUE,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    admin_email VARCHAR(255) NOT NULL,
    CONSTRAINT chk_single_bootstrap_record CHECK (id = 1)
);

-- 2. Columna generada y restricción UNIQUE que impide más de un ADMIN a nivel de base de datos
-- En SQL estándar, múltiples valores NULL están permitidos en UNIQUE (para TEACHER),
-- pero solo puede existir un único valor 'ADMIN'.
ALTER TABLE teachers ADD COLUMN admin_singleton VARCHAR(10) GENERATED ALWAYS AS (CASE WHEN role = 'ADMIN' THEN 'ADMIN' ELSE NULL END) STORED;
ALTER TABLE teachers ADD CONSTRAINT uq_teachers_single_admin UNIQUE (admin_singleton);
