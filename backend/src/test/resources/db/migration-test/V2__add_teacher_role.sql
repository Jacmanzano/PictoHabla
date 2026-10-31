-- ========================================================
-- PictoHabla CAA - Migración V2: Sistema de Roles (ADMIN, TEACHER)
-- ========================================================

-- Añadir columna role a teachers con valor predeterminado 'TEACHER'
-- En PostgreSQL y H2, añadir columna NOT NULL con DEFAULT asigna
-- automáticamente el valor por defecto a todos los registros existentes.
ALTER TABLE teachers ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'TEACHER';

-- Restricción CHECK para impedir roles no válidos en la base de datos
ALTER TABLE teachers ADD CONSTRAINT chk_teachers_role CHECK (role IN ('ADMIN', 'TEACHER'));

-- Índice para optimizar consultas por rol
CREATE INDEX idx_teachers_role ON teachers(role);
