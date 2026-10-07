-- Cambios al esquema posteriores a las tablas iniciales (usuarios, eventos, inscripciones, asistencias).
-- El backend usa spring.jpa.hibernate.ddl-auto=validate: la base de datos NO se genera desde las entidades,
-- por eso estos cambios se versionan aquí. Todas las sentencias se pueden volver a ejecutar sin riesgo.

-- Categorías de eventos (HU-04)
CREATE TABLE IF NOT EXISTS categorias (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(50) NOT NULL UNIQUE,
    activo     BOOLEAN     NOT NULL DEFAULT TRUE,
    creado_en  TIMESTAMP   NOT NULL DEFAULT NOW()
);
ALTER TABLE eventos ADD COLUMN IF NOT EXISTS categoria_id BIGINT REFERENCES categorias(id);

-- Usuarios: cuenta habilitada y código estudiantil
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS codigo_estudiantil VARCHAR(20);

-- Logs de operaciones críticas (RN13 y RNF07)
CREATE TABLE IF NOT EXISTS logs (
    id          BIGSERIAL PRIMARY KEY,
    fecha       TIMESTAMP   NOT NULL DEFAULT NOW(),
    usuario_id  BIGINT REFERENCES usuarios(id),
    accion      VARCHAR(50) NOT NULL,
    entidad     VARCHAR(50) NOT NULL,
    entidad_id  BIGINT,
    resultado   VARCHAR(20) NOT NULL,
    motivo      TEXT
);
CREATE INDEX IF NOT EXISTS idx_logs_entidad ON logs(entidad, entidad_id);
CREATE INDEX IF NOT EXISTS idx_logs_usuario ON logs(usuario_id);

-- RN01: un usuario solo puede tener UNA inscripción ACTIVA por evento.
-- Se reemplaza la restricción única total por un índice parcial, para poder volver a inscribirse
-- después de cancelar (las inscripciones canceladas se conservan, RN14).
ALTER TABLE inscripciones DROP CONSTRAINT IF EXISTS uq_estudiante_evento;
CREATE UNIQUE INDEX IF NOT EXISTS idx_inscripcion_activa_unica
    ON inscripciones (estudiante_id, evento_id) WHERE estado = 'ACTIVA';

-- Seguridad a nivel de fila (el backend se conecta con un rol que la omite)
ALTER TABLE categorias ENABLE ROW LEVEL SECURITY;
ALTER TABLE logs       ENABLE ROW LEVEL SECURITY;
