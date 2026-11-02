-- =============================================================================
-- 005: recuperación de contraseña por correo y cambio de contraseña desde el perfil (HU-19).
--
-- El backend usa ddl-auto=validate: este script se debe ejecutar en Supabase ANTES de desplegar
-- el backend que trae estos cambios, o la aplicación no arranca.
-- Todas las sentencias se pueden volver a ejecutar sin riesgo.
-- =============================================================================

-- Momento del último cambio de contraseña. Los JWT emitidos antes de esa fecha dejan de ser válidos,
-- así una contraseña restablecida cierra las sesiones abiertas con la anterior.
alter table usuarios add column if not exists password_actualizada_en timestamp;

-- Enlaces de recuperación. Solo se guarda el hash SHA-256 del token: quien lea la tabla no puede
-- usar los enlaces. Un token sirve una sola vez (usado_en) y vence (expira_en).
create table if not exists tokens_recuperacion (
    id          bigserial   primary key,
    usuario_id  bigint      not null references usuarios (id),
    token_hash  varchar(64) not null unique,
    expira_en   timestamp   not null,
    usado_en    timestamp,
    creado_en   timestamp   not null default now()
);
create index if not exists idx_tokens_recuperacion_usuario on tokens_recuperacion (usuario_id);

-- Sin políticas: solo el backend (rol postgres, que ignora RLS) puede leer y escribir.
alter table tokens_recuperacion enable row level security;
