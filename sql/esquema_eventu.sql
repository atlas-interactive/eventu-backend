-- =============================================================================
-- EventU - Script único de la base de datos (PostgreSQL / Supabase)
--
-- Crea el esquema completo y carga los datos de demostración. Reemplaza a los
-- scripts 001 a 006.
--
-- Se puede ejecutar en una base vacía o en una que ya tenga el esquema: cada
-- sentencia verifica si el objeto existe, así que volver a correrlo no duplica
-- tablas ni datos.
--
-- El backend usa ddl-auto=validate (Hibernate no crea tablas): este script debe
-- ejecutarse antes de arrancar el backend.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. Tablas
-- -----------------------------------------------------------------------------

create table if not exists usuarios (
    id                      bigserial    primary key,
    nombre                  varchar(100) not null,
    correo                  varchar(100) not null unique,
    password_hash           varchar(255) not null,
    rol                     varchar(20)  not null
                            constraint usuarios_rol_check check (rol in ('USUARIO', 'ORGANIZADOR', 'ADMIN')),
    creado_en               timestamp    default current_timestamp,
    codigo_institucional    varchar(20),
    activo                  boolean      not null default true,
    password_actualizada_en timestamp
);

-- Confirmación de correo. Si la columna no existía, las cuentas que ya estaban
-- quedan confirmadas para que puedan seguir iniciando sesión.
do $$
begin
    if not exists (select 1 from information_schema.columns
                   where table_schema = 'public' and table_name = 'usuarios'
                     and column_name = 'correo_confirmado_en') then
        alter table usuarios add column correo_confirmado_en timestamp;
        update usuarios set correo_confirmado_en = coalesce(creado_en, now());
    end if;
end $$;

create table if not exists categorias (
    id        bigserial   primary key,
    nombre    varchar(50) not null unique,
    activo    boolean     not null default true,
    creado_en timestamp   not null default now()
);

create table if not exists eventos (
    id                bigserial    primary key,
    titulo            varchar(150) not null,
    descripcion       text,
    fecha_inicio      timestamp    not null,
    ubicacion         varchar(150) not null,
    cupos_maximos     integer      not null,
    cupos_disponibles integer      not null,
    estado            varchar(20)  default 'PUBLICADO'
                      constraint eventos_estado_check check (estado in ('PUBLICADO', 'CANCELADO')),
    certificable      boolean      default false,
    organizador_id    bigint       references usuarios (id),
    creado_en         timestamp    default current_timestamp,
    categoria_id      bigint       references categorias (id)
);

create table if not exists inscripciones (
    id                bigserial   primary key,
    usuario_id        bigint      references usuarios (id),
    evento_id         bigint      references eventos (id),
    codigo_qr         text        not null unique,
    estado            varchar(20) default 'ACTIVA'
                      constraint inscripciones_estado_check check (estado in ('ACTIVA', 'CANCELADA')),
    fecha_inscripcion timestamp   default current_timestamp
);

create table if not exists asistencias (
    id             bigserial primary key,
    inscripcion_id bigint    unique references inscripciones (id),
    fecha_registro timestamp default current_timestamp
);

create table if not exists logs (
    id         bigserial   primary key,
    fecha      timestamp   not null default now(),
    usuario_id bigint      references usuarios (id),
    accion     varchar(50) not null,
    entidad    varchar(50) not null,
    entidad_id bigint,
    resultado  varchar(20) not null,
    motivo     text
);

-- Enlaces de recuperación de contraseña y de confirmación de correo. Solo se
-- guarda el hash SHA-256 del token: quien lea la tabla no puede usar los enlaces.
create table if not exists tokens_recuperacion (
    id         bigserial   primary key,
    usuario_id bigint      not null references usuarios (id),
    token_hash varchar(64) not null unique,
    expira_en  timestamp   not null,
    usado_en   timestamp,
    creado_en  timestamp   not null default now()
);

create table if not exists tokens_confirmacion_correo (
    id         bigserial   primary key,
    usuario_id bigint      not null references usuarios (id),
    token_hash varchar(64) not null unique,
    expira_en  timestamp   not null,
    usado_en   timestamp,
    creado_en  timestamp   not null default now()
);


-- -----------------------------------------------------------------------------
-- 2. Índices
-- -----------------------------------------------------------------------------

create index if not exists idx_eventos_fecha_inicio on eventos (fecha_inicio);
create index if not exists idx_eventos_categoria    on eventos (categoria_id);

-- RN01: una sola inscripción ACTIVA por usuario y evento. Las canceladas se
-- conservan (RN14), por eso la unicidad es parcial.
create unique index if not exists idx_inscripcion_activa_unica
    on inscripciones (usuario_id, evento_id) where estado = 'ACTIVA';

create index if not exists idx_tokens_recuperacion_usuario on tokens_recuperacion (usuario_id);
create index if not exists idx_tokens_confirmacion_usuario on tokens_confirmacion_correo (usuario_id);


-- -----------------------------------------------------------------------------
-- 3. Seguridad
--
-- Supabase expone cada tabla por su API pública. RLS activo y sin políticas:
-- solo el backend (rol postgres, que ignora RLS) puede leer y escribir.
-- -----------------------------------------------------------------------------

alter table usuarios                   enable row level security;
alter table categorias                 enable row level security;
alter table eventos                    enable row level security;
alter table inscripciones              enable row level security;
alter table asistencias                enable row level security;
alter table logs                       enable row level security;
alter table tokens_recuperacion        enable row level security;
alter table tokens_confirmacion_correo enable row level security;


-- -----------------------------------------------------------------------------
-- 4. Datos iniciales
-- -----------------------------------------------------------------------------

insert into categorias (nombre) values ('Académico'), ('Cultural'), ('Deportivo')
on conflict (nombre) do nothing;


-- -----------------------------------------------------------------------------
-- 5. Datos de demostración
--
-- Contraseña de las dos cuentas: EventuDemo2026 (hash BCrypt de costo 10).
-- Cambiarla desde el perfil si la base es la de producción.
--
-- Ningún endpoint crea administradores. Para tener uno, registrar la cuenta y
-- promoverla aquí:
--   update usuarios set rol = 'ADMIN' where correo = 'correo.del.admin@unillanos.edu.co';
-- -----------------------------------------------------------------------------

insert into usuarios (nombre, correo, password_hash, rol, correo_confirmado_en) values
    ('Organizador Demo', 'organizador.demo@unillanos.edu.co',
     '$2a$10$7TKUeylZgFL73ClnoC34rePfIiPLqCwaCPSOAhMyfMB.4.4wGyTTW', 'ORGANIZADOR', now()),
    ('Usuario Demo', 'usuario.demo@unillanos.edu.co',
     '$2a$10$7TKUeylZgFL73ClnoC34rePfIiPLqCwaCPSOAhMyfMB.4.4wGyTTW', 'USUARIO', now())
on conflict (correo) do nothing;

-- Eventos futuros (hora de Colombia, la misma con la que compara el backend).
insert into eventos (titulo, descripcion, fecha_inicio, ubicacion, cupos_maximos, cupos_disponibles,
                     estado, certificable, organizador_id, categoria_id)
select d.titulo, d.descripcion,
       date_trunc('day', now() at time zone 'America/Bogota') + d.dentro_de,
       d.ubicacion, d.cupos, d.cupos, 'PUBLICADO', d.certificable,
       (select id from usuarios where correo = 'organizador.demo@unillanos.edu.co'),
       (select id from categorias where nombre = d.categoria)
from (values
    ('Semana de la Ingeniería de Sistemas', 'Charlas y talleres sobre desarrollo de software y datos.',
     interval '7 days 9 hours', 'Auditorio Barcelona, sede Barcelona', 120, true, 'Académico'),
    ('Taller de React y TypeScript', 'Taller práctico para construir una aplicación web paso a paso.',
     interval '10 days 14 hours', 'Sala de cómputo 3, bloque de Ingeniería', 25, true, 'Académico'),
    ('Noche cultural llanera', 'Muestra de música, danza y gastronomía de la región.',
     interval '14 days 18 hours', 'Plazoleta central, sede Barcelona', 200, false, 'Cultural'),
    ('Torneo interfacultades de microfútbol', 'Inscripción individual; los equipos se arman el día del torneo.',
     interval '21 days 8 hours', 'Coliseo de la universidad', 60, false, 'Deportivo')
) as d(titulo, descripcion, dentro_de, ubicacion, cupos, certificable, categoria)
where not exists (select 1 from eventos e where e.titulo = d.titulo);

-- Inscripción del usuario de demostración al taller, con su código QR.
insert into inscripciones (usuario_id, evento_id, codigo_qr, estado, fecha_inscripcion)
select u.id, e.id, 'EVENTU-QR-' || upper(gen_random_uuid()::text), 'ACTIVA', now() at time zone 'America/Bogota'
from usuarios u, eventos e
where u.correo = 'usuario.demo@unillanos.edu.co'
  and e.titulo = 'Taller de React y TypeScript'
  and not exists (select 1 from inscripciones i where i.usuario_id = u.id and i.evento_id = e.id);

update eventos e
set cupos_disponibles = e.cupos_maximos
    - (select count(*) from inscripciones i where i.evento_id = e.id and i.estado = 'ACTIVA')
where e.titulo = 'Taller de React y TypeScript';
