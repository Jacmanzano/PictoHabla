-- ========================================================
-- PictoHabla CAA - Migración Inicial V1 (PostgreSQL & H2 comp.)
-- ========================================================

-- Tabla: Profesores y Administradores
CREATE TABLE teachers (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token VARCHAR(255),
    verification_token_expires_at TIMESTAMP WITH TIME ZONE,
    password_reset_token VARCHAR(255),
    password_reset_expires_at TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_teachers_email ON teachers(email);

-- Tabla: Aulas de Comunicación
CREATE TABLE classrooms (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_classrooms_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE RESTRICT
);

CREATE INDEX idx_classrooms_teacher_id ON classrooms(teacher_id);

-- Tabla: Alumnos (Identificación por Pseudónimo - Privacidad por Diseño)
-- Ningún dato personal identificable (sin nombre real, sin email, sin teléfono, sin datos médicos)
CREATE TABLE students (
    id UUID PRIMARY KEY,
    classroom_id UUID NOT NULL,
    pseudonym_code VARCHAR(50) NOT NULL UNIQUE,
    nickname VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_students_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id) ON DELETE RESTRICT
);

CREATE INDEX idx_students_classroom_id ON students(classroom_id);
CREATE INDEX idx_students_pseudonym ON students(pseudonym_code);

-- Tabla: Refresh Tokens Hash (Seguridad & Rotación de Tokens)
-- Nunca almacena el token en texto plano, solo su digest SHA-256
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_token_hash VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_refresh_tokens_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_teacher_id ON refresh_tokens(teacher_id);
