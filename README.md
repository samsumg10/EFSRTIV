# BBQ Reservas — Spring Boot

Sistema de reservas de zonas de parrilla (BBQ) para varias empresas. Migración a Spring Boot + MySQL + Bootstrap
del sistema Laravel `bbq`. Proyecto EFSRT IV — CIBERTEC.

## Requisitos

- Docker Desktop (abierto antes de ejecutar los comandos)
- Conexión a internet la primera vez (Docker descarga las imágenes y Maven las dependencias; las páginas cargan Bootstrap y FullCalendar por CDN)
- Java 21 **solo** si vas a correr la app fuera de Docker (opción B)

## Cómo levantarlo

**Opción A: todo en Docker (no necesitas Java instalado)**

```powershell
docker compose up -d --build   # MySQL 8.4 + la app compilada con Java 21 → http://localhost:8081
```

La primera vez tarda unos minutos, mientras descarga las imágenes y compila. Después de cambiar el código,
vuelve a ejecutar el mismo comando para recompilar.
Logs de la app: `docker compose logs -f app`.

**Opción B: MySQL en Docker y la app desde tu IDE o terminal (para desarrollar)**

```powershell
docker compose up -d mysql     # solo MySQL en localhost:3309 (bbq_db / myuser / secret)
.\mvnw.cmd spring-boot:run     # la app en http://localhost:8081 (requiere Java 21)
```

No mezcles las dos opciones: ambas usan el puerto 8081. Si antes levantaste la opción A, primero
ejecuta `docker compose stop app`.

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
