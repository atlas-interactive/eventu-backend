-- =============================================================================
-- EventU - Esquema completo de la base de datos (PostgreSQL / Supabase)
--
-- Sirve para crear la base de datos desde cero (por ejemplo, un entorno nuevo).
-- Refleja el estado FINAL, despues de la migracion 002 y de la limpieza 004.
-- En la base de datos que ya existe NO hace falta ejecutarlo: se usan 002, 003 y 004.
-- =============================================================================

create table usuarios (
    id                   bigserial    primary key,
    nombre               varchar(100) not null,
    correo               varchar(100) not null unique,
    password_hash        varchar(255) not null,
    rol                  varchar(20)  not null
                         constraint usuarios_rol_check check (rol in ('USUARIO', 'ORGANIZADOR', 'ADMIN')),
    creado_en            timestamp    default current_timestamp,
    codigo_institucional varchar(20)
);

create table categorias (
    id        bigserial   primary key,
    nombre    varchar(50) not null unique,
    activo    boolean     not null default true,
    creado_en timestamp   not null default now()
);

create table eventos (
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

create table inscripciones (
    id                bigserial   primary key,
    usuario_id        bigint      references usuarios (id),
    evento_id         bigint      references eventos (id),
    codigo_qr         text        not null unique,
    estado            varchar(20) default 'ACTIVA'
                      constraint inscripciones_estado_check check (estado in ('ACTIVA', 'CANCELADA')),
    fecha_inscripcion timestamp   default current_timestamp
);

-- RN01: un usuario solo puede tener una inscripcion ACTIVA por evento.
-- Las canceladas se conservan (RN14), por eso la unicidad es parcial y no una restriccion normal.
create unique index idx_inscripcion_activa_unica
    on inscripciones (usuario_id, evento_id) where estado = 'ACTIVA';

create table asistencias (
    id             bigserial primary key,
    inscripcion_id bigint    unique references inscripciones (id),
    fecha_registro timestamp default current_timestamp
);

create table logs (
    id         bigserial   primary key,
    fecha      timestamp   not null default now(),
    usuario_id bigint      references usuarios (id),
    accion     varchar(50) not null,
    entidad    varchar(50) not null,
    entidad_id bigint,
    resultado  varchar(20) not null,
    motivo     text
);

-- Seguridad: sin politicas, solo el backend (rol postgres, que ignora RLS) puede leer y escribir.
alter table usuarios      enable row level security;
alter table categorias    enable row level security;
alter table eventos       enable row level security;
alter table inscripciones enable row level security;
alter table asistencias   enable row level security;
alter table logs          enable row level security;

-- Categorias iniciales
insert into categorias (nombre) values ('Académico'), ('Cultural'), ('Deportivo')
on conflict (nombre) do nothing;
