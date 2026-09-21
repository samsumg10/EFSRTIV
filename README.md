# BBQ Reservas — Spring Boot

Sistema de reservas de zonas de parrilla (BBQ) para varias empresas. Migración a Spring Boot + MySQL + Bootstrap
del sistema Laravel `bbq`. Proyecto EFSRT IV — CIBERTEC.

## Requisitos

- Java 21
- Docker Desktop (para MySQL 8.4)
- Conexión a internet la primera vez (Maven descarga dependencias; las páginas cargan Bootstrap y FullCalendar por CDN)

## Cómo levantarlo

```powershell
docker compose up -d          # MySQL 8.4 en localhost:3309 (bbq_db / myuser / secret)
.\mvnw.cmd spring-boot:run    # la app en http://localhost:8081
```

Al primer arranque se crean las tablas (`schema.sql`) y los datos demo (`DataInitializer`).
Para empezar de cero: `docker compose down -v` y volver a levantar.

Variables opcionales: `PORT`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`.

## Usuarios demo

| Panel | URL | Correo | Contraseña |
|-------|-----|--------|------------|
| Admin de plataforma | http://localhost:8081/admin/login | admin@bbq.com | admin123 |
| Empresa (administrador) | http://localhost:8081/company/login | demo@bbq.com | demo1234 |
| Empresa (empleado, solo sus instalaciones) | http://localhost:8081/company/login | empleado@bbq.com | demo1234 |

## Páginas

| Área | URL |
|------|-----|
| Admin | `/admin/companies`, `/admin/roles` |
| Empresa | `/company/profile`, `/company/employees`, `/company/facilities` |
| | `/company/facilities/{id}/locations`, `/company/facilities/{id}/products` |
| | `/company/locations/{id}/bookings` (calendario), `/company/locations/{id}/closeddays` |
| | `/company/facilities/{id}/booking/getBookingsByFacility` (lista de reservas) |
| Portal del cliente (sin login) | `/facility/{uuid}` → `/location/{uuid}` → `/booking-details/{uuid}` |

Los enlaces al portal salen en los botones **🔗 Portal** de instalaciones y ubicaciones.

## Arquitectura

Igual que el proyecto `asociacion`: `controllers → services → repositories → models (+ DTO)` y `security/` con JWT.
El mismo Spring Boot sirve la API REST (`/api/admin/**`, `/api/company/**`, `/api/public/**`) y las páginas HTML
(`src/main/resources/static`), que usan JavaScript simple (`fetch`) y los estilos de `asociacion-frontend`.

Diseño completo, reglas de negocio y endpoints: `PLAN_IMPLEMENTACION.md`.

## Reglas principales

- Se reserva con 2 a 60 días de anticipación; una reserva ocupa el día completo.
- Capacidad por día = `max_person` de la ubicación (suma de personas); si está vacío, la ubicación es exclusiva (1 reserva por día).
- No se reserva en días cerrados; no se puede cerrar un día que ya tiene reservas activas.
- Reservas del portal: quedan **pendientes** de pago (transferencia) con plazo de 3 días; si vencen, pasan a **expiradas**
  (tarea programada cada hora). Reservas registradas desde el panel: quedan **pagadas**.
- Cada empresa solo ve sus datos; un empleado (rol tipo 2) solo ve las instalaciones que tiene asignadas.
