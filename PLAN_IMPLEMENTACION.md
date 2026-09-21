# Plan de implementación — BBQ Reservas (Laravel → Spring Boot)

Migración de la lógica del sistema de reservas BBQ (`C:\NEOTECH\bbq`, Laravel 10 + PostgreSQL) a
**Spring Boot 4 + MySQL 8.4 + Bootstrap**, con la arquitectura del proyecto `asociacion`
y los estilos de `asociacion-frontend`.

> **Cómo usar este plan:** en cada sesión pide *"Implementa la Parte N de PLAN_IMPLEMENTACION.md"*.
> Lee solo §1–§8 si hace falta contexto y la sección de esa parte. Al terminar, marca la parte
> como ✅ en `CLAUDE.md`.

---

## 1. Alcance: rutas a migrar

| URL | Qué hace en el original (Laravel) | Qué hará en Spring Boot |
|-----|------------------------------------|-------------------------|
| `/admin/companies` | `Admin\CompanyController`: CRUD de empresas + su representante (empleado) | CRUD empresa + representante (rol tipo 1) |
| `/admin/roles` | `Admin\RoleController`: nombre + matriz JSON de permisos | CRUD **simple**: nombre + tipo |
| `/company/profile` | `Company\CompanyController`: datos de la empresa + representante | Datos de la empresa + mi cuenta (nombre, email, teléfono, contraseña) |
| `/company/employees` | `EmployeeController`: CRUD, rol, instalaciones asignadas | Igual (rol + instalaciones en `employee_facility`) |
| `/company/facilities` | `FacilityController`: CRUD de instalaciones | Igual + enlaces a ubicaciones, productos, reservas y portal |
| `/company/facilities/{id}/locations` | `LocationController`: CRUD de ubicaciones | CRUD: nombre, descripción, capacidad, precio |
| `/company/locations/{id}/bookings` | `BookingController@index`: calendario FullCalendar + reserva manual | Calendario **FullCalendar 5.11.3** + reserva manual + detalle |
| `/company/locations/{id}/closeddays` | `ClosedDayController`: días cerrados con validación de solapes | Igual |
| `/company/facilities/{id}/products` | `ProductController`: CRUD de productos | CRUD: nombre, precio |
| `/company/facilities/{id}/booking/getBookingsByFacility` | Lista de reservas con filtros + estadísticas | Igual (filtros, estadísticas, paginación, cambio de estado) |
| `/facility/{uuid}` | Portal: instalación + sus ubicaciones | Igual (público, sin login) |
| `/location/{uuid}` | Portal: calendario, personas, productos, datos del cliente → carrito | Igual; el carrito vive en `sessionStorage` |
| `/booking-details/{uuid}` | Portal: resumen + pago con tarjeta (fincode) | Resumen + **confirmar reserva** (pago por transferencia simulado) |

Además: `/admin/login`, `/company/login`, y las redirecciones `/admin` → `/admin/companies`, `/company` → `/company/facilities`.

### Fuera de alcance (no existen en el nuevo diagrama)
Planes/franjas horarias y reglas de precio por fecha, imágenes, elementos de formulario dinámicos,
avisos, áreas, impuestos (`system_settings`), pasarela fincode, correos, PDF, i18n (ja/en),
dashboard de ventas, export CSV, hoteles multi-día, carpas, tipos de persona (adulto/niño/infante),
stock / meses disponibles / límite diario de productos.

---

## 2. Decisiones de diseño

Las marcadas con ⚠️ **cambian el diagrama**; el usuario las confirmó el 2026-09-20.

| # | Decisión | Motivo |
|---|----------|--------|
| D1 ⚠️ | Agregar `bookings.location_id` (FK a `locations`) | El diagrama no liga la reserva con la ubicación: sin esto no hay calendario por ubicación, ni lista por instalación, ni control de capacidad |
| D2 ⚠️ | Agregar `locations.price DECIMAL(10,2)` = **precio por reserva** | `sales.location_price` necesita una fuente de precio; `locations` no tiene precio |
| D3 ⚠️ | Quitar `UNIQUE` de `bookings.email` (queda como índice) | Con UNIQUE un cliente solo podría reservar una vez en toda la vida del sistema |
| D4 ⚠️ | Todos los `id` y FK como `BIGINT` | El diagrama mezcla INT y BIGINT (`employee_facility`, `closed_days.facility_id`, `bookings.employee_id`) y MySQL rechaza FK entre tipos distintos |
| D5 ⚠️ | Admin de plataforma con credenciales en `application.properties` (`app.admin.email` / `app.admin.password`) | El diagrama no tiene tabla `admins`. Alternativa: agregar tabla `admins(id, name, email, password, timestamps)` |
| D6 | `roles.type`: **1 = Administrador de empresa** (todo en su empresa), **2 = Empleado** (solo instalaciones asignadas; sin Perfil ni Empleados; no crea ni borra instalaciones) | Sustituye la matriz JSON de permisos y el truco `facility_id = 0` ("todas") del original |
| D7 | Sin pasarela de pago. Reserva del portal → venta **PENDIENTE** con fecha límite; la empresa la marca **PAGADA**; un job horario pasa a **EXPIRADA** las vencidas. Reserva manual de la empresa → **PAGADA** (como el original) | fincode es una pasarela japonesa; `sales` no tiene columnas de pasarela |
| D8 | Front = páginas HTML estáticas + JS vanilla (`fetch`) servidas por el mismo Spring Boot **en las mismas URLs**; JWT en `localStorage` como `asociacion-frontend`; Bootstrap 5.3 (CDN) para grid y modales + `app.css` con los estilos de `asociacion-frontend` encima | "Vistas simples con HTML y CSS" + la arquitectura REST/JWT de `asociacion` |
| D9 | Calendario: **FullCalendar 5.11.3** por CDN (la misma librería y versión que el original) | Es JS puro: funciona igual con Spring Boot |
| D10 | `server.port=8081` (variable `PORT`) | El Docker de `bbq` ocupa el 80 y otro proyecto Spring el 8080. Para URLs idénticas basta `PORT=80` con ese Docker apagado |
| D11 | Soft delete con `deleted_at`: `BaseEntity.markDeleted()` y los repositorios filtran `deletedAt IS NULL` (como `flg = 1` en `asociacion`). Al borrar empresa o empleado se renombra el email a `email-{id}` (igual que el original) | Filtrar a mano permite seguir leyendo productos/empleados borrados desde reservas antiguas; el renombrado libera el UNIQUE |
| D12 | Una reserva = **un día completo** (`bookings.date`). `locations.max_person` = capacidad diaria (suma de personas de reservas activas). `max_person` NULL = ubicación **exclusiva** (1 reserva por día) | No hay planes/franjas en el diagrama. Reproduce los tipos 3 (por capacidad) y 1 (bloqueo) del original |
| D13 | Días cerrados solo por ubicación; `closed_days.facility_id` se llena con la instalación de la ubicación; `end_date` vacío = mismo día que `start_date` | `closed_days.location_id` es NOT NULL en el diagrama |
| D14 | Esquema en `schema.sql` (`CREATE TABLE IF NOT EXISTS`) ejecutado al arrancar, con `ddl-auto=none` | La BD queda exactamente como el diagrama; `asociacion` usa `create`, que borra los datos en cada arranque |
| D15 | Solo español; moneda `S/` en una constante (`app.currency`) | Simplifica; el original era ja/en/es con ¥ |
| D16 | Una sola implementación de disponibilidad y precios (`AvailabilityService` + `BookingService`) para portal y empresa | El original duplica esa lógica en `PortalController` y `BookingController` |

---

## 3. Base de datos (MySQL 8.4) — `src/main/resources/schema.sql`

Diagrama del usuario + D1–D4. `remember_token` se conserva, pero no se usa (con JWT no hace falta).

```sql
CREATE TABLE IF NOT EXISTS roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  type INT NOT NULL,                              -- 1 admin empresa, 2 empleado
  created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL, deleted_at TIMESTAMP NULL
);
CREATE TABLE IF NOT EXISTS companies (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  phone VARCHAR(20) NOT NULL,
  address VARCHAR(255) NULL,
  status SMALLINT NOT NULL DEFAULT 1,             -- 1 activa, 0 inactiva
  uuid CHAR(36) NOT NULL UNIQUE,
  url VARCHAR(255) NULL,
  created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL, deleted_at TIMESTAMP NULL
);
CREATE TABLE IF NOT EXISTS employees (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(20) NULL,
  role_id BIGINT NOT NULL,
  remember_token VARCHAR(100) NULL,
  created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL, deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_employees_company FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_employees_role    FOREIGN KEY (role_id)    REFERENCES roles(id)
);
CREATE TABLE IF NOT EXISTS facilities (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  address VARCHAR(255) NULL,
  description TEXT NULL,
  uuid CHAR(36) NOT NULL UNIQUE,
  created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL, deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_facilities_company FOREIGN KEY (company_id) REFERENCES companies(id)
);
CREATE TABLE IF NOT EXISTS employee_facility (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  facility_id BIGINT NOT NULL,
  created_at TIMESTAMP NULL, deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_ef_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
  CONSTRAINT fk_ef_facility FOREIGN KEY (facility_id) REFERENCES facilities(id)
);
CREATE TABLE IF NOT EXISTS locations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  facility_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  description TEXT NULL,
  uuid CHAR(36) NOT NULL UNIQUE,
  max_person INT NULL,                            -- NULL = exclusiva (1 reserva/día)
  price DECIMAL(10,2) NOT NULL DEFAULT 0,         -- D2
  created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL, deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_locations_facility FOREIGN KEY (facility_id) REFERENCES facilities(id)
);
CREATE TABLE IF NOT EXISTS closed_days (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  location_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NULL,
  reason TEXT NULL,
  facility_id BIGINT NULL,
  created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL, deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_cd_location FOREIGN KEY (location_id) REFERENCES locations(id),
  CONSTRAINT fk_cd_facility FOREIGN KEY (facility_id) REFERENCES facilities(id)
);
CREATE TABLE IF NOT EXISTS products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  facility_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL, deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_products_facility FOREIGN KEY (facility_id) REFERENCES facilities(id)
);
CREATE TABLE IF NOT EXISTS bookings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,                    -- D1
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL,                    -- D3: sin UNIQUE
  phone VARCHAR(20) NOT NULL,
  date DATE NOT NULL,
  employee_id BIGINT NULL,                        -- quién la creó (NULL = portal)
  uuid CHAR(36) NOT NULL UNIQUE,
  number_persons INT NOT NULL,
  created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL, deleted_at TIMESTAMP NULL,
  INDEX idx_bookings_email (email),
  INDEX idx_bookings_location_date (location_id, date),
  CONSTRAINT fk_bookings_company  FOREIGN KEY (company_id)  REFERENCES companies(id),
  CONSTRAINT fk_bookings_location FOREIGN KEY (location_id) REFERENCES locations(id),
  CONSTRAINT fk_bookings_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);
CREATE TABLE IF NOT EXISTS booking_details (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  booking_id BIGINT NOT NULL,
  product_id BIGINT NULL,
  price DECIMAL(10,2) NOT NULL,                   -- precio unitario congelado
  total DECIMAL(10,2) NOT NULL,
  quantity INT NOT NULL,
  created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL, deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_bd_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
  CONSTRAINT fk_bd_product FOREIGN KEY (product_id) REFERENCES products(id)
);
CREATE TABLE IF NOT EXISTS sales (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  booking_id BIGINT NOT NULL UNIQUE,              -- 1:1
  location_price DECIMAL(10,2) NOT NULL,
  product_total DECIMAL(10,2) NOT NULL,
  total DECIMAL(10,2) NOT NULL,
  status SMALLINT NOT NULL,                       -- 1 pendiente, 2 pagado, 3 cancelado, 4 expirado
  paid_at TIMESTAMP NULL,
  payment_deadline_date DATE NULL,
  created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL, deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_sales_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);
```

**Semillas** (`DataInitializer`, patrón de `asociacion`, solo si la tabla está vacía): roles `Administrador` (type 1)
y `Empleado` (type 2); empresa demo "BBQ Demo" + empleado `demo@bbq.com` / `demo1234` (type 1);
una instalación, dos ubicaciones y tres productos de ejemplo (así las Partes 3–10 se pueden probar sin cargar datos a mano).

---

## 4. Arquitectura y estructura

Igual que `asociacion`: `controllers → services → repositories → models (+ models/DTO)` y `security/` con JWT.
Los controladores nunca devuelven entidades: siempre DTO `XxxRequestDTO` / `XxxResponseDTO`.

```
Proyecto/
├── CLAUDE.md · PLAN_IMPLEMENTACION.md
├── pom.xml · mvnw · mvnw.cmd · .mvn/        (wrapper copiado de asociacion)
├── docker-compose.yml                       (MySQL 8.4 alternativo en :3309)
├── Dockerfile
└── src/main/
    ├── java/com/cibertec/bbq/
    │   ├── BbqApplication.java              (@EnableJpaAuditing, @EnableScheduling)
    │   ├── DataInitializer.java
    │   ├── models/                          (BaseEntity + 11 entidades)
    │   │   ├── DTO/
    │   │   └── AppConstants.java            (tipos de rol, estados de venta, ventana de reserva)
    │   ├── repositories/
    │   ├── services/
    │   ├── controllers/
    │   │   ├── PageController.java          (URL → forward a HTML)
    │   │   ├── admin/                       (AdminAuth, AdminCompany, AdminRole)
    │   │   ├── company/                     (CompanyAuth, Profile, Employee, Facility, Location,
    │   │   │                                 Product, ClosedDay, Booking)
    │   │   └── portal/PortalController.java
    │   ├── security/                        (Constants, JWTAuthenticationConfig,
    │   │                                     JWTAuthorizationFilter, WebSecurityConfig,
    │   │                                     AuthUser, CurrentUser)
    │   └── exceptions/                      (GlobalExceptionHandler, ForbiddenException,
    │                                         BusinessException)
    └── resources/
        ├── application.properties · schema.sql
        └── static/
            ├── css/app.css                  (estilos de asociacion-frontend)
            ├── js/api.js · js/ui.js · js/layout.js · js/calendar.js
            ├── js/pages/*.js                (uno por página)
            └── pages/{admin,company,portal}/*.html
```

**Entidades** (`@Data`, `@Entity`, `@EntityListeners(AuditingEntityListener.class)`, relaciones `@ManyToOne(fetch = LAZY)`):
`BaseEntity` (`@MappedSuperclass`: id, createdAt, updatedAt, deletedAt), `Role`, `Company`, `Employee`,
`EmployeeFacility` (sin `updated_at`: no extiende `BaseEntity`), `Facility`, `Location`, `ClosedDay`,
`Product`, `Booking`, `BookingDetail`, `Sale` (`@OneToOne` con `Booking`).

**Transacciones:** services con `@Transactional` (lectura: `readOnly = true`); `spring.jpa.open-in-view=false`.

**`application.properties`** (patrón de `asociacion`, con variables de entorno):
```properties
spring.application.name=bbq
server.port=${PORT:8081}
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3309/bbq_db?createDatabaseIfNotExist=true}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:myuser}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:secret}
spring.jpa.hibernate.ddl-auto=none
spring.sql.init.mode=always
spring.jpa.open-in-view=false
app.admin.email=${ADMIN_EMAIL:admin@bbq.com}
app.admin.password=${ADMIN_PASSWORD:admin123}
```
La BD por defecto es el contenedor de `docker-compose.yml` (MySQL 8.4.7 en :3309, `myuser`/`secret`).
Para usar el MySQL 8.4 local basta con las variables `SPRING_DATASOURCE_*`.

---

## 5. Seguridad (JWT, igual que `asociacion` + datos de tenant)

- Login: `POST /api/admin/login` (compara con `app.admin.*`) y `POST /api/company/login` (empleado + BCrypt;
  rechaza si `companies.status = 0`, como `auth.company_inactive` del original).
- Claims: `sub` (email), `id`, `name`, `companyId`, `roleType`, `authorities` =
  `ROLE_ADMIN` | `ROLE_COMPANY_ADMIN` (type 1) | `ROLE_EMPLOYEE` (type 2). Expira en 8 h.
- `JWTAuthorizationFilter` pone como principal un `AuthUser(id, companyId, roleType)`;
  `CurrentUser.get()` lo lee en los services.
- `WebSecurityConfig` (`@EnableMethodSecurity`):
  - `permitAll`: GET de las URLs de páginas, `/pages/**`, `/css/**`, `/js/**`, `/api/public/**`,
    los dos login, dispatcher `FORWARD` y `ERROR`.
  - `/api/admin/**` → `ROLE_ADMIN`; `/api/company/**` → `ROLE_COMPANY_ADMIN` o `ROLE_EMPLOYEE`.
  - `@PreAuthorize("hasRole('COMPANY_ADMIN')")` en perfil, empleados, y alta/baja de instalaciones.
- **Aislamiento por empresa** (el original lo hace a mano en cada controlador; aquí en un solo sitio):
  `AccessService.facility(id)` / `.location(id)` / `.booking(id)` cargan el recurso, verifican
  `companyId` y, si `roleType = 2`, que la instalación esté en `employee_facility`; si no → 403.
- Las páginas son públicas; `layout.js` redirige al login si no hay token o expiró (lee `exp` del JWT).
  Tokens separados: `bbq_admin_token` y `bbq_company_token`.
- `GlobalExceptionHandler` (`@RestControllerAdvice`, pendiente en `asociacion`):
  validación → **422** `{"errors": {"campo": ["mensaje"]}}` (mismo formato que Laravel);
  `BusinessException` → 422; `NoSuchElementException` → 404; `ForbiddenException` / `AccessDeniedException` → 403.

---

## 6. Reglas de negocio (dominio)

| ID | Regla | Origen en Laravel |
|----|-------|-------------------|
| RN-01 | Ventana de reserva: `hoy + 2 ≤ fecha ≤ hoy + 60` (portal y empresa) | `BookingController@store`, `PortalController@addToCart` |
| RN-02 | No se reserva en día cerrado de la ubicación (`start_date ≤ fecha ≤ end_date`) | `PlanHelper::closedDays` |
| RN-03 | Capacidad: personas activas del día + solicitadas ≤ `max_person`. Si `max_person` es NULL: máximo 1 reserva activa por día. "Activa" = no borrada y venta en estado 1 o 2 | `checkAvailabilityByNumberPerson`, `MaxPeopleHelper` |
| RN-04 | Días llenos (próximos 90 días) se marcan en el calendario: una sola consulta `GROUP BY date` | `DaysFullyBookedHelper` (hacía N consultas) |
| RN-05 | Concurrencia: al crear una reserva se bloquea la fila de la ubicación (`@Lock(PESSIMISTIC_WRITE)`) en una transacción **`READ_COMMITTED`** (con `REPEATABLE READ`, el default de MySQL, la suma de ocupación leía la foto previa al bloqueo y se sobrevendía el último cupo; comprobado) | `lockForUpdate` en `store` |
| RN-06 | Precios solo en servidor: `location_price = location.price`; `product_total = Σ price × qty`; `total = location_price + product_total`; `booking_details` congela precio unitario | `addToCart` / `store` |
| RN-07 | Productos de la reserva deben pertenecer a la instalación de la ubicación; cantidad ≥ 0 (las 0 se ignoran) | `ProductValidationsInBookingsHelper` |
| RN-08 | Venta desde portal: estado 1 PENDIENTE, `payment_deadline_date = min(hoy + 3, fecha − 1)`. Desde empresa: estado 2 PAGADO, `paid_at = now`, `employee_id` = usuario | `PortalController@store`, `BookingController@store` |
| RN-09 | Transiciones: PENDIENTE → PAGADO (`paid_at`) · PENDIENTE/PAGADO → CANCELADO · PENDIENTE → EXPIRADO (job horario si venció el plazo). Cancelar/expirar libera capacidad; la reserva no se borra | `VerifyPayments` (borraba la reserva) |
| RN-10 | Portal: 403 si la empresa está inactiva (`status = 0`) | `PortalController@show*` |
| RN-11 | Días cerrados: `start ≤ end` y sin solaparse con otros de la misma ubicación | `ClosedDayController::betweenDays` |
| RN-12 | Empresa nueva ⇒ se crea su representante (empleado con el primer rol de tipo 1). Representante = empleado tipo 1 más antiguo | `Admin\CompanyController@store` |
| RN-13 | No se puede borrar un rol en uso, ni borrarse a uno mismo, ni dejar la empresa sin empleados tipo 1 | nuevo (sustituye el flag `representative`) |
| RN-14 | Al borrar empresa/empleado: email → `email-{id}` y soft delete (la empresa también renombra los emails de sus empleados) | `Admin\CompanyController@destroy`, `EmployeeController@destroy` |
| RN-15 | No se puede registrar un día cerrado sobre fechas que ya tienen reservas activas (hay que cancelarlas antes) | nuevo |

Estados de venta y badge CSS: 1 Pendiente `badge-pendiente` · 2 Pagado `badge-pagado` · 3 Cancelado `badge-anulado` · 4 Expirado `badge-inactivo`.

---

## 7. API REST

Todas devuelven JSON. `{fid}` = facility, `{lid}` = location.

**Admin** (`ROLE_ADMIN`)
| Método | URL | Uso |
|--------|-----|-----|
| POST | `/api/admin/login` | login (público) |
| GET · POST | `/api/admin/companies` | listar · crear empresa + representante |
| GET · PUT · DELETE | `/api/admin/companies/{id}` | ver (incluye representante) · editar · borrar |
| GET · POST | `/api/admin/roles` | listar · crear |
| GET · PUT · DELETE | `/api/admin/roles/{id}` | ver · editar · borrar (RN-13) |

**Empresa** (`ROLE_COMPANY_ADMIN` / `ROLE_EMPLOYEE`)
| Método | URL | Uso |
|--------|-----|-----|
| POST | `/api/company/login` | login (público) |
| GET | `/api/company/me` | usuario, empresa, roleType (para el sidebar) |
| GET · PUT | `/api/company/profile` | empresa + mi cuenta (solo tipo 1) |
| GET | `/api/company/roles` | roles para el select de empleados |
| GET · POST · GET/PUT/DELETE `{id}` | `/api/company/employees` | CRUD (solo tipo 1); body incluye `facilityIds[]` |
| GET · POST · GET/PUT/DELETE `{id}` | `/api/company/facilities` | CRUD (tipo 2: solo lectura de las asignadas) |
| GET · POST · GET/PUT/DELETE `{id}` | `/api/company/facilities/{fid}/locations` | CRUD |
| GET · POST · GET/PUT/DELETE `{id}` | `/api/company/facilities/{fid}/products` | CRUD |
| GET · POST · GET/PUT/DELETE `{id}` | `/api/company/locations/{lid}/closeddays` | CRUD + RN-11 |
| GET | `/api/company/locations/{lid}` | datos de la ubicación (cabeceras) |
| GET | `/api/company/locations/{lid}/calendar-info` | ubicación, productos, minDate/maxDate |
| GET | `/api/company/locations/{lid}/calendar-days?start=&end=` | días cerrados y días llenos del rango visible (se pide en cada cambio de mes) |
| GET | `/api/company/locations/{lid}/availability?date=` | capacidad restante del día |
| GET | `/api/company/locations/{lid}/bookings?start=&end=` | eventos FullCalendar |
| POST | `/api/company/locations/{lid}/bookings` | reserva manual (RN-01…08) |
| GET | `/api/company/bookings/{id}` | detalle: cliente, productos, venta |
| PATCH | `/api/company/bookings/{id}/status` | `{status: 2 \| 3}` (RN-09) |
| GET | `/api/company/facilities/{fid}/bookings` | lista filtrada + estadísticas + paginación |

**Portal** (público)
| Método | URL | Uso |
|--------|-----|-----|
| GET | `/api/public/facilities/{uuid}` | instalación + empresa + ubicaciones (RN-10) |
| GET | `/api/public/locations/{uuid}` | ubicación + productos + ventana (minDate/maxDate) |
| GET | `/api/public/locations/{uuid}/calendar-days?start=&end=` | días cerrados y llenos del rango visible |
| GET | `/api/public/locations/{uuid}/availability?date=` | capacidad restante |
| POST | `/api/public/locations/{uuid}/quote` | valida y cotiza sin guardar (equivale a `addToCart`) |
| POST | `/api/public/locations/{uuid}/bookings` | crea reserva + detalles + venta PENDIENTE |
| GET | `/api/public/bookings/{uuid}` | comprobante de la reserva |

---

## 8. Frontend

- **Estilos:** `static/css/app.css` = `styles.css` + `shared.css` + `layout.css` + `login.css` + `.modal-card`
  de `asociacion-frontend` (azul `#1a237e`, fondo `#f5f6fa`, sidebar 240 px, cards y tablas con radio 12 px,
  botones con radio 8 px, badges tipo píldora). Orden de carga: Bootstrap 5.3 (CDN jsDelivr) → `app.css`,
  para que `app.css` sobrescriba `.btn`, `.card`, `.badge`, `.alert` y `table` con el aspecto de `asociacion`.
  Los modales de Bootstrap se estilan como `.modal-card` (título `#1a237e`).
- **Layout** (`layout.js`): pinta sidebar + topbar como `layout.html` de `asociacion` (emoji + texto,
  botón ☰ para colapsar, usuario/rol arriba a la derecha, "Cerrar sesión" abajo). Menú admin: 🏢 Empresas, 🛡️ Roles.
  Menú empresa: 👤 Perfil, 👥 Empleados (solo tipo 1), 🏕️ Instalaciones. Las páginas hijas marcan activo
  "Instalaciones" y muestran migas de pan (Instalaciones › {instalación} › Ubicaciones …).
- **Portal:** sin sidebar; topbar blanca con el nombre de la empresa, contenido `max-width: 1100px`, misma paleta.
- **JS:** vanilla, un archivo por página (`js/pages/company-locations.js`, …). Los ids se leen de
  `location.pathname` con regex. `api.js`: `api.get/post/put/patch/del` con `Authorization: Bearer`;
  401 → login del área; 422 → `ui.showErrors(form, errors)` pinta los errores bajo cada campo.
  `ui.js`: `alert`, `confirm` (modal), `money`, `date`, `escapeHtml` (**siempre** al usar `innerHTML`:
  los datos del portal los escribe cualquiera).
- **Calendario** (`calendar.js`): FullCalendar 5.11.3 (`main.min.js`/`main.min.css` + `locales/es.js` desde jsDelivr).
  Config del original: `initialView: 'dayGridMonth'`, `headerToolbar: {left:'prev,next today', center:'title',
  right:'dayGridMonth,listMonth'}`, `fixedWeekCount: false`, `eventDisplay: 'block'`, eventos con
  `events: function(info, ok, fail)` usando `fetch` con token. `dayCellDidMount` colorea: cerrado `#ccccff`
  (color del original), lleno `#ffebee`, fuera de ventana gris. Botones del calendario con `#1a237e`.
  Color del evento por estado de venta (pendiente `#f9a825`, pagado `#2e7d32`; cancelado/expirado ocultos
  salvo con el filtro "mostrar cancelados").

---

## 9. Partes de implementación

> **Estado (2026-09-20): las 11 partes están implementadas y probadas.** Esta sección queda como referencia de qué contiene cada parte
> y de cómo probarla. Diferencias con lo planeado: puerto 8081; soft delete filtrado en repositorios (D11);
> `calendar-days` separado de `calendar-info`; RN-05 con `READ_COMMITTED`; RN-15 nueva.

Dependencias: **1 → 2 → (3, 4, 5 en paralelo) → 6 → (7, 8, 9) → 10 → 11**.
Responsable sugerido según `Proyecto EFSRT IV.txt`: Admin = Josue · CRUDs empresa = Joseph · Reservas = Daniel · Clientes = Marco.

### Parte 1 — Esqueleto y base de datos  *(común)*
- `pom.xml` copiado de `asociacion` (Spring Boot 4.0.5, Java 21, webmvc, data-jpa, security, validation,
  mysql-connector-j, lombok, jjwt 0.11.5); `groupId com.cibertec`, `artifactId bbq`. Copiar `mvnw`, `mvnw.cmd`, `.mvn/`.
- `docker-compose.yml` (mysql:8.4.7, `bbq_db`, puerto 3309), `Dockerfile`, `.gitignore`, `application.properties` (§4).
- `schema.sql` (§3), `BbqApplication`, `models/BaseEntity` + 11 entidades (soft delete D11),
  `models/AppConstants`, 11 repositorios (incluye ya las consultas de disponibilidad, calendario y lista de las Partes 6–8).
- `DataInitializer` con las semillas de §3 (el BCrypt `PasswordEncoder` se declara aquí en un `@Configuration`
  temporal o directamente en `WebSecurityConfig` de la Parte 2; en la Parte 1 basta un `SecurityFilterChain` que permita todo).
- **Probar:** `.\mvnw.cmd spring-boot:run` arranca; en MySQL existen las 11 tablas con las FK y las semillas.

### Parte 2 — Seguridad, login y base del front  *(común)*
- `security/*` (§5), `exceptions/*`, `AdminAuthController`, `CompanyAuthController` + `AuthService`,
  `GET /api/company/me`.
- `PageController` con **todas** las URLs de §1 (forward a `/pages/...html`) y las redirecciones `/admin`, `/company`.
- `css/app.css`, `js/api.js`, `js/ui.js`, `js/layout.js`; `pages/admin/login.html`, `pages/company/login.html`
  (estilo `login.css`: degradado azul, tarjeta centrada, 🔥 "BBQ Reservas").
- Páginas vacías con layout para cada URL (título + "en construcción") para que la navegación funcione desde ya.
- **Probar:** login admin y empresa → token en `localStorage` → sidebar correcto; `/api/company/me` con token 200,
  sin token 401; empresa con `status = 0` no puede entrar; un empleado tipo 2 no ve "Empleados" ni "Perfil".

### Parte 3 — Admin: empresas y roles  *(Josue)*
- `AdminCompanyController` + `CompanyService`: listar (nombre, email, teléfono, estado, fecha), crear
  (empresa con `uuid` + representante con el primer rol tipo 1), editar (contraseña opcional), borrar (RN-14).
  Validaciones: nombre, email único (empresa y empleado), teléfono `^[0-9+\-]+$`, url `http(s)://`, estado 0/1,
  contraseña ≥ 8 al crear.
- `AdminRoleController` + `RoleService`: CRUD nombre + tipo (select 1 Administrador de empresa / 2 Empleado); RN-13.
- `pages/admin/companies.html` (tabla + modal con dos bloques "Empresa" y "Representante"),
  `pages/admin/roles.html` (tabla + modal). Mismo patrón de `puestos-list` de `asociacion-frontend`
  (buscador, tabla, badges, acciones).
- **Probar:** crear empresa → su representante entra en `/company/login`; desactivarla → ya no entra;
  borrar un rol en uso → mensaje de error.

### Parte 4 — Empresa: perfil y empleados  *(Joseph)*
- `ProfileController`: GET/PUT empresa (nombre, email, teléfono, dirección, url; `uuid` solo lectura) +
  mi cuenta (nombre, email, teléfono, contraseña opcional).
- `EmployeeController` + `EmployeeService`: CRUD de la propia empresa; campos nombre, email, teléfono,
  contraseña + confirmación (obligatoria al crear), rol, `facilityIds[]` (sincroniza `employee_facility`
  con soft delete de las quitadas). RN-13, RN-14. `GET /api/company/roles`.
- `pages/company/profile.html` (dos cards con `form-grid`), `pages/company/employees.html`
  (tabla con badge del rol + modal con checkboxes de instalaciones; se ocultan si el rol es tipo 1).
- **Probar:** empleado tipo 2 con una instalación asignada solo ve esa en `/company/facilities`;
  no se puede editar un empleado de otra empresa (403).

### Parte 5 — Empresa: instalaciones, ubicaciones y productos  *(Joseph)*
- `AccessService` (§5) — la usan todas las partes siguientes.
- `FacilityController`: CRUD (nombre*, email*, teléfono*, dirección, descripción; `uuid` al crear).
  Tipo 2: solo lectura de las asignadas.
- `LocationController`: CRUD (nombre*, descripción, capacidad `max_person` opcional ≥ 1, precio* ≥ 0; `uuid` al crear).
- `ProductController`: CRUD (nombre*, precio* ≥ 0 con 2 decimales).
- Páginas: `facilities.html` (tabla con acciones 📍 Ubicaciones · 🍖 Productos · 📋 Reservas · 🔗 Portal `/facility/{uuid}` ·
  Editar · Eliminar), `locations.html` (acciones 📅 Reservas · 🚫 Días cerrados · 🔗 Portal `/location/{uuid}` · Editar · Eliminar),
  `products.html`. Migas de pan en las hijas.
- **Probar:** CRUD completo en las tres páginas; `/company/facilities/{id de otra empresa}/locations` → 403.

### Parte 6 — Días cerrados y motor de reservas  *(Joseph: días cerrados · Daniel: motor)*
- `ClosedDayController` + `ClosedDayService`: CRUD con RN-11 y D13; `pages/company/closeddays.html`
  (tabla inicio/fin/motivo + modal con inputs `date`).
- `AvailabilityService` (una sola implementación para empresa y portal):
  `isInWindow(date)`, `isClosed(location, date)`, `closedDates(location, from, to)`,
  `occupied(location, date)` (suma de personas de reservas activas), `remaining(location, date)`,
  `fullyBookedDates(location, from, to)` (una consulta `GROUP BY date`).
- `BookingService.validateAndPrice(location, BookingRequestDTO)` → `BookingQuoteDTO`
  (RN-01, 02, 03, 06, 07; errores 422 con el campo que falla). Aún no guarda.
- Endpoints `calendar-info` y `availability` de la empresa.
- **Probar:** días solapados → 422; `calendar-info` devuelve días cerrados expandidos y días llenos;
  `availability` resta bien las personas.

### Parte 7 — Calendario de reservas de la ubicación  *(Daniel)*  → `/company/locations/{id}/bookings`
- `BookingService.create(location, dto, origen)` con transacción + `@Lock(PESSIMISTIC_WRITE)` sobre la ubicación
  (RN-05) → `Booking` (`uuid`), `BookingDetail`s, `Sale` (RN-08). Lo reutiliza la Parte 10.
- `GET .../bookings?start=&end=` (eventos: `id`, `title` = "Nombre (N pers.)", `start` = fecha, `allDay`,
  `color` por estado), `POST .../bookings`, `GET /api/company/bookings/{id}`, `PATCH .../status` (RN-09).
- `pages/company/bookings.html` + `js/calendar.js` (§8):
  - cabecera con ubicación, capacidad, precio y leyenda de colores;
  - clic en un día válido → modal "Nueva reserva": nombre, email, teléfono, personas (muestra la capacidad restante),
    productos con cantidad, totales en vivo; guardar → refetch de eventos;
  - clic en un evento → modal de detalle: cliente, fecha, personas, tabla de productos, totales, badge del estado,
    botones "Marcar como pagado" / "Cancelar reserva".
- **Probar:** reservar hasta llenar la capacidad → el día queda marcado lleno y un nuevo intento da 422;
  día cerrado no clicable; cancelar libera capacidad.

### Parte 8 — Lista de reservas por instalación  *(Daniel)*  → `/company/facilities/{id}/booking/getBookingsByFacility`
- `GET /api/company/facilities/{fid}/bookings` con filtros del original: fechas de reserva `resStart`/`resEnd`
  (el original usaba hoy…hoy; aquí la página abre con "desde hoy" y sin fin, para ver todas las próximas), fechas de registro `regStart`/`regEnd` (`created_at`), `uuid`, `status`, `locationId`;
  selector de instalación entre las autorizadas; paginación de 10 (`Pageable`). Consulta con
  `Specification` o `@Query` + join a `sales`.
- Estadísticas (como `getBookingStatistics`): total de reservas, monto total, pagadas (cantidad y monto),
  pendientes (cantidad y monto) → tarjetas KPI con el estilo `kpi-card` del dashboard de `asociacion`.
- `pages/company/booking-list.html`: barra de filtros, KPIs, tabla (código, cliente, ubicación, fecha, personas,
  total, estado, registrado), modal de detalle reutilizando el de la Parte 7 (mismas acciones de estado).
- **Probar:** los filtros combinados cuadran con los KPI; un empleado tipo 2 no puede consultar una instalación no asignada.

### Parte 9 — Portal: instalación y ubicación  *(Marco)*  → `/facility/{uuid}`, `/location/{uuid}`
- `PortalController` + `PortalService`: `GET /api/public/facilities/{uuid}`, `GET /api/public/locations/{uuid}`,
  `availability`, `POST quote` (usa `BookingService.validateAndPrice`). RN-10.
- `pages/portal/facility.html`: cabecera (instalación, descripción, dirección, teléfono, email, empresa) +
  grid de tarjetas de ubicaciones (nombre, descripción, capacidad, precio, botón "Reservar" → `/location/{uuid}`).
- `pages/portal/location.html`: datos de la ubicación + FullCalendar (misma config; días fuera de ventana,
  cerrados y llenos deshabilitados) → al elegir día muestra la capacidad restante; formulario: personas,
  productos con cantidad, nombre, email, teléfono; totales en vivo. "Continuar" → `POST quote`;
  si es válido guarda `{locationUuid, date, numberPersons, products[], customer}` en `sessionStorage`
  (`bbq_cart`) y navega a `/booking-details/{uuid}`; si no, pinta los errores 422.
- **Probar:** empresa inactiva → página de "no disponible"; no se puede elegir un día cerrado, lleno o fuera de ventana.

### Parte 10 — Portal: confirmación de la reserva  *(Marco)*  → `/booking-details/{uuid}`
- `POST /api/public/locations/{uuid}/bookings` (usa `BookingService.create` con origen PORTAL → venta PENDIENTE) y
  `GET /api/public/bookings/{uuid}`.
- `pages/portal/booking-details.html`: lee `bbq_cart` (si falta o es de otra ubicación → vuelve a `/location/{uuid}`),
  vuelve a cotizar para mostrar precios actuales, muestra el resumen con el estilo `recibo-*` de `pago-detalle.css`
  de `asociacion-frontend`; "Confirmar reserva" → crea → panel de éxito con código (`uuid`), fecha límite de pago e
  instrucciones de transferencia (texto fijo); limpia el carrito. Si otro cliente llenó el día → error 422 y enlace para volver.
- **Probar:** la reserva aparece en el calendario de la empresa (Parte 7) como pendiente y en la lista (Parte 8);
  dos confirmaciones simultáneas con la última plaza → solo una se guarda.

### Parte 11 — Cierre  *(común)*
- `SaleExpirationJob` (`@Scheduled(cron = "0 0 * * * *")`): PENDIENTE con plazo vencido → EXPIRADO (RN-09).
- Revisión de 403 entre empresas en todos los endpoints, textos en español, estados vacíos ("No hay reservas"),
  diseño responsive (sidebar colapsable).
- `README.md` (cómo levantar, usuarios demo, URLs), colección Postman opcional (como `asociacion`),
  actualizar `CLAUDE.md`.
- **Prueba integral:** admin crea empresa → la empresa crea instalación, ubicación, productos, empleado y un día cerrado →
  el cliente reserva desde el portal → la empresa la ve en el calendario y la lista y la marca como pagada.
