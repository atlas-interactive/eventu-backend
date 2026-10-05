-- =============================================================================
-- Limpieza 004: ejecutar DESPUES de desplegar el codigo nuevo y comprobar que funciona.
--
--   A) borra los datos que creo el script de pruebas (pruebas-backend.sh): todo lo que lleva el
--      prefijo "prueba." en el correo o "Prueba " en el nombre de la categoria;
--   B) elimina la columna usuarios.activo, que el codigo nuevo ya no usa (se retiro la nocion de
--      cuenta deshabilitada);
--   C) agrega las categorias iniciales que falten.
--
-- No toca al usuario administrador real (admineventu@gmail.com).
-- =============================================================================

begin;

-- A) datos de prueba
create temp table _insc_prueba on commit drop as
    select i.id
    from inscripciones i
    where i.usuario_id in (select id from usuarios where correo like 'prueba.%')
       or i.evento_id in (select id from eventos
                          where organizador_id in (select id from usuarios where correo like 'prueba.%'));

delete from asistencias   where inscripcion_id in (select id from _insc_prueba);
delete from inscripciones where id in (select id from _insc_prueba);
delete from eventos       where organizador_id in (select id from usuarios where correo like 'prueba.%');
delete from logs          where usuario_id in (select id from usuarios where correo like 'prueba.%');
delete from categorias    where nombre like 'Prueba %'
                            and not exists (select 1 from eventos e where e.categoria_id = categorias.id);
delete from usuarios      where correo like 'prueba.%';

-- B) cuentas deshabilitadas: ya no existen
alter table usuarios drop column if exists activo;

-- C) categorias iniciales
insert into categorias (nombre) values ('Académico'), ('Cultural'), ('Deportivo')
on conflict (nombre) do nothing;

commit;
