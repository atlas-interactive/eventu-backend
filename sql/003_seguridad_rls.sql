-- =============================================================================
-- Seguridad 003: activar Row Level Security en las tablas que aun no la tienen.
--
-- Por que: Supabase expone cada tabla por una API publica. Sin RLS, cualquiera que tenga la clave
-- "anon" del proyecto puede leer o modificar usuarios (incluido password_hash), eventos e inscripciones.
-- Supabase lo marca como un problema CRITICO.
--
-- Por que es seguro para el backend: se conecta con el rol "postgres", que ignora RLS. Ya esta
-- comprobado: categorias y logs tienen RLS activado y el backend escribe en ambas sin problema.
-- La web y el movil no hablan con Supabase directamente (solo con el backend), asi que tampoco se afectan.
--
-- Se activa SIN politicas a proposito: nadie, salvo el backend, debe poder acceder por la API publica.
-- Se puede ejecutar en cualquier momento, no depende del despliegue.
-- =============================================================================

alter table usuarios      enable row level security;
alter table eventos       enable row level security;
alter table inscripciones enable row level security;
alter table asistencias   enable row level security;
