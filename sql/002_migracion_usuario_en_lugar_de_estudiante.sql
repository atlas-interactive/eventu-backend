-- =============================================================================
-- Migracion 002: "estudiante" pasa a "usuario" y se retira el estado FINALIZADO.
--
-- CUANDO EJECUTARLA: en el mismo momento en que se despliega el codigo nuevo
-- (rama refactor/usuario-y-criterios). El codigo viejo y el nuevo no son compatibles:
--   - el codigo viejo busca la columna estudiante_id y el rol ESTUDIANTE;
--   - el codigo nuevo busca usuario_id y el rol USUARIO.
-- Todo ocurre en una sola transaccion: si algo falla, no se aplica nada.
-- =============================================================================

begin;

-- Revisiones previas: si algo no cumple, se detiene con un mensaje claro
do $$
begin
    if exists (select 1 from eventos where estado = 'FINALIZADO') then
        raise exception 'Hay eventos FINALIZADO; decide a que estado pasarlos antes de migrar';
    end if;
end $$;

-- 1) Roles: ESTUDIANTE -> USUARIO.
--    SUPERADMIN existia en la base de datos pero no en el codigo (Hibernate fallaria al leer ese usuario):
--    pasa a ADMIN, que es el rol que el codigo conoce.
alter table usuarios drop constraint if exists usuarios_rol_check;
update usuarios set rol = 'USUARIO' where rol = 'ESTUDIANTE';
update usuarios set rol = 'ADMIN'   where rol = 'SUPERADMIN';
alter table usuarios
    add constraint usuarios_rol_check check (rol in ('USUARIO', 'ORGANIZADOR', 'ADMIN'));

-- 2) inscripciones.estudiante_id -> usuario_id
--    (el indice parcial idx_inscripcion_activa_unica se actualiza solo con el cambio de nombre)
alter table inscripciones rename column estudiante_id to usuario_id;
alter table inscripciones rename constraint inscripciones_estudiante_id_fkey to inscripciones_usuario_id_fkey;

-- 3) usuarios.codigo_estudiantil -> codigo_institucional
alter table usuarios rename column codigo_estudiantil to codigo_institucional;

-- 4) Estados de un evento: FINALIZADO se quita porque ningun proceso lo asigna
alter table eventos drop constraint if exists eventos_estado_check;
alter table eventos
    add constraint eventos_estado_check check (estado in ('PUBLICADO', 'CANCELADO'));

commit;

-- Comprobacion (ejecutar despues):
--   select rol, count(*) from usuarios group by rol;
--   select estado, count(*) from eventos group by estado;
--   select column_name from information_schema.columns where table_name = 'inscripciones' order by ordinal_position;
