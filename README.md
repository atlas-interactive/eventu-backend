# EventU - Backend API

Servicio Backend central desarrollado en Java Spring Boot para la plataforma **EventU** (Sistema de gestión de eventos estudiantiles y registro de asistencia mediante QR).

## Arquitectura y Tecnologías
* **Lenguaje:** Java 17+
* **Framework:** Spring Boot (Spring Web, Spring Data JPA, Spring Security)
* **Base de Datos:** PostgreSQL en la nube via **Supabase**
* **Seguridad:** Autenticación y Autorización basada en JSON Web Tokens (JWT)
* **Generación de QR:** (Por definir)

## Configuración Local

### Prerrequisitos
* JDK 17 o superior
* Apache Maven
* Cuenta en Supabase con el script DDL ejecutado

### Variables de Entorno (`application.properties`)
Configura el archivo `src/main/resources/application.properties` con tus credenciales de Supabase:

```properties
server.port=8080

# Conexión JDBC Supabase
spring.datasource.url=jdbc:postgresql://<TU_HOST_SUPABASE>:5432/postgres?sslmode=require
spring.datasource.username=postgres
spring.datasource.password=<TU_PASSWORD_SUPABASE>

# JPA / Hibernate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
