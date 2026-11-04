-- =============================================================================
-- 006: confirmación del correo al registrarse.
--
-- El backend usa ddl-auto=validate: este script se debe ejecutar en Supabase ANTES de desplegar
-- el backend que trae estos cambios, o la aplicación no arranca.
-- Todas las sentencias se pueden volver a ejecutar sin riesgo.
--
-- La confirmación solo se exige si eventu.registro.confirmar-correo=true
-- (variable EVENTU_REGISTRO_CONFIRMAR_CORREO en Render). Por defecto está apagada.
-- =============================================================================

-- Momento en que el usuario abrió el enlace de confirmación (null = sin confirmar).
-- Las cuentas que ya existían al agregar la columna quedan confirmadas, para no bloquear a nadie.
-- La actualización corre solo la vez que se crea la columna: si el script se repite, no confirma
-- las cuentas nuevas que todavía no abrieron su enlace.
do $$
begin
    if not exists (select 1 from information_schema.columns
                   where table_schema = 'public' and table_name = 'usuarios' and column_name = 'correo_confirmado_en') then
        alter table usuarios add column correo_confirmado_en timestamp;
        update usuarios set correo_confirmado_en = coalesce(creado_en, now());
    end if;
end $$;

-- Enlaces de confirmación. Igual que en tokens_recuperacion, solo se guarda el hash SHA-256 del token.
create table if not exists tokens_confirmacion_correo (
    id          bigserial   primary key,
    usuario_id  bigint      not null references usuarios (id),
    token_hash  varchar(64) not null unique,
    expira_en   timestamp   not null,
    usado_en    timestamp,
    creado_en   timestamp   not null default now()
);
create index if not exists idx_tokens_confirmacion_usuario on tokens_confirmacion_correo (usuario_id);

-- Sin políticas: solo el backend (rol postgres, que ignora RLS) puede leer y escribir.
alter table tokens_confirmacion_correo enable row level security;
