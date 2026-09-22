# CLAUDE.md — BBQ Reservas (Spring Boot)

Migración del sistema de reservas BBQ (Laravel, `C:\NEOTECH\bbq`) a Spring Boot + MySQL + Bootstrap.
Proyecto EFSRT IV — CIBERTEC, 6.º ciclo.

## Idioma

Responder **siempre en español de Perú** (tuteo, vocabulario peruano: "computadora", "celular", "ustedes"; nada de "vosotros" ni "vale").

**El diseño completo está en `PLAN_IMPLEMENTACION.md`** (decisiones §2, esquema §3, seguridad §5, reglas §6, API §7).
Para cambios, leer solo la sección que aplique; no hace falta releer el proyecto Laravel.

## Estado

Partes 1–11 del plan **implementadas y probadas** (2026-09-20). Decisiones D1–D5 (cambios al diagrama) confirmadas.

Verificado: compilación, todos los endpoints con curl (validaciones 422, 403 entre empresas, restricciones del rol tipo 2),
concurrencia (5 reservas simultáneas por el último cupo → solo entran las que caben), job de expiración, y un recorrido
de todas las páginas con Edge headless (puppeteer) sin errores de JavaScript.

No hay tests automatizados en el repo (el proyecto de referencia tampoco los tiene).

## Proyectos de referencia

- Arquitectura: `F:\COMPUTACION_E_INFORMATICA_CIBERTEC\5.1 -  QUINTO CICLO - JAPON\MODULO I\DESARROLLO DE APLICACIONES WEB I - QUINTO CICLO\Proyectos\PROYECTO FINAL\asociacion`
- Estilos: `...\PROYECTO FINAL\asociacion-frontend` (`styles.css`, `shared.css`, `layout.css`, `login.css`, `pago-detalle.css`)
- Sistema original: `C:\NEOTECH\bbq\public_html` (Laravel).

## Stack y arranque

Spring Boot 4.0.5 · Java 21 · Maven wrapper · MySQL 8.4.7 en Docker (:3309, `bbq_db`, `myuser`/`secret`)
· Spring Data JPA · Spring Security + JWT (jjwt 0.11.5) · Lombok · Bootstrap 5.3 + FullCalendar 5.11.3 por CDN.

`docker compose up -d --build` (MySQL + app con Java 21, multi-stage `Dockerfile`) o `docker compose up -d mysql`
+ `.\mvnw.cmd spring-boot:run` → http://localhost:8081
(el 80 lo ocupa el Docker de `bbq` y el 8080 otro proyecto Spring).
Demo: admin `admin@bbq.com`/`admin123` · empresa `demo@bbq.com`/`demo1234` (tipo 1) · `empleado@bbq.com`/`demo1234` (tipo 2).

## Mapa del código

- `controllers/PageController` — URL original → `forward:/pages/...html`.
- `controllers/{admin,company,portal}` — API REST. `CompanyBookingController` agrupa calendario, reserva manual, detalle, cambio de estado y lista por instalación.
- `services/AccessService` — aislamiento por empresa e instalaciones asignadas; **todo** service de `/api/company` pasa por aquí.
- `services/AvailabilityService` — ventana de reserva, días cerrados, ocupación y días llenos.
- `services/BookingService` — `quote` (valida y cotiza) y `create` (bloqueo + guardado); lo usan empresa y portal.
- `services/SaleExpirationJob` — pendientes vencidas → expiradas (cada hora y al arrancar).
- `static/js/{api,ui,layout,calendar,booking-detail,portal}.js` — piezas compartidas; `static/js/pages/*.js` — una por página.

## Convenciones

- Paquete `com.cibertec.bbq`; DTO `XxxRequestDTO` / `XxxResponseDTO`; los controladores nunca devuelven entidades.
- Entidades con `@Getter/@Setter` (no `@Data`); `open-in-view=false` → el mapeo a DTO va dentro de services `@Transactional`.
- Soft delete con `deleted_at`: `entity.markDeleted()` y los repositorios filtran `deletedAt IS NULL`. Esquema en `schema.sql` (`ddl-auto=none`).
- Crear reservas: transacción `READ_COMMITTED` + `LocationRepository.lockById` (con `REPEATABLE READ` se sobrevende el último cupo).
- Errores: `BusinessException(campo, mensaje)` → 422 `{"message", "errors": {"campo": [..]}}`; el front los pinta con `Ui.showErrors`.
- Front: JS vanilla; `Ui.esc` siempre que se use `innerHTML`; textos en español; moneda `S/`.
- Evitar abrir un modal de Bootstrap encima de otro (conflicto de foco): confirmar dentro del mismo modal.
