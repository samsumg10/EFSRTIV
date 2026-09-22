# Reglas de negocio — BBQ Reservas

Sistema de reservas de zonas de parrilla (BBQ). Varias **empresas** usan la misma plataforma. Cada una publica sus
**instalaciones** y **ubicaciones**, y sus clientes reservan desde un portal público sin crear cuenta.

> Este documento describe lo que **el sistema hace hoy**: cada regla se revisó contra el código (`services/`, `models/DTO/`).
> Los IDs `RN-01`…`RN-15` son los mismos de `PLAN_IMPLEMENTACION.md` §6; del `RN-16` en adelante son reglas
> que estaban en el código pero no en esa tabla.

---

## 1. Actores

| Actor | Quién es | Cómo entra | Alcance |
|---|---|---|---|
| **Administrador de la plataforma** | Quien opera el sistema completo (el dueño de BBQ Reservas) | `/admin/login`. Hay **un solo** administrador y su correo y contraseña se configuran en `application.properties` (`app.admin.*`), no en la BD | Todas las empresas y los roles |
| **Administrador de empresa** | Empleado con rol de **tipo 1**. El más antiguo de la empresa se considera el **representante** | `/company/login` | Toda **su** empresa |
| **Empleado** | Empleado con rol de **tipo 2** (personal de una sede, recepción, etc.) | `/company/login` | Solo las **instalaciones que tiene asignadas** |
| **Cliente** | Persona que quiere reservar una zona de parrilla | Portal público `/facility/{uuid}` → `/location/{uuid}`, **sin login** | Solo ve y reserva; no puede consultar ni cancelar sus reservas |
| **Sistema (proceso automático)** | `SaleExpirationJob` | Corre **cada hora** y cada vez que arranca la aplicación | Vence las reservas pendientes que no se pagaron a tiempo |

### 1.1 Qué puede hacer cada actor

| Función | Admin plataforma | Admin empresa (tipo 1) | Empleado (tipo 2) | Cliente |
|---|:-:|:-:|:-:|:-:|
| Crear, editar y eliminar empresas (con su representante) | ✅ | — | — | — |
| Crear, editar y eliminar roles | ✅ | — | — | — |
| Ver y editar el perfil de la empresa y su propia cuenta | — | ✅ | ❌ | — |
| Gestionar empleados y asignarles instalaciones | — | ✅ | ❌ | — |
| Crear, editar y eliminar instalaciones | — | ✅ | ❌ | — |
| Ver instalaciones | — | todas las de su empresa | solo las asignadas | las de empresas activas |
| Gestionar ubicaciones, productos y días cerrados | — | ✅ | ✅ (en sus instalaciones) | — |
| Ver el calendario y la lista de reservas | — | ✅ | ✅ (en sus instalaciones) | — |
| Registrar una reserva manual (queda pagada) | — | ✅ | ✅ | — |
| Marcar una reserva como pagada o cancelarla | — | ✅ | ✅ | — |
| Reservar desde el portal (queda pendiente de pago) | — | — | — | ✅ |
| Ver el comprobante de su reserva | — | — | — | ✅ (con el enlace) |

---

## 2. Conceptos del dominio

| Concepto | Significado |
|---|---|
| **Empresa** | Negocio cliente de la plataforma. Puede estar **activa** (1) o **inactiva** (0) |
| **Rol** | Catálogo **global**, lo administra el admin de la plataforma. Cada rol tiene un nombre libre y un **tipo**: 1 = Administrador de empresa, 2 = Empleado |
| **Instalación** | Sede o local de la empresa. Tiene un enlace público propio (`uuid`) |
| **Ubicación** | Zona reservable dentro de una instalación (por ejemplo, "Parrilla 1"). Tiene un **precio por reserva** y una **capacidad diaria** (`max_person`) |
| **Producto** | Adicional que se vende con la reserva (carbón, bebidas, etc.). Pertenece a una instalación |
| **Día cerrado** | Rango de fechas en que una ubicación no recibe reservas |
| **Reserva** | Una ubicación, **un día completo**, un número de personas y los datos del cliente |
| **Venta** | Relación 1 a 1 con la reserva: precios congelados y **estado del pago** |

---

## 3. Acceso y seguridad

| ID | Regla |
|---|---|
| RN-16 | El login pide un correo válido y una contraseña. Si fallan, el mensaje es siempre el mismo ("Correo o contraseña incorrectos.") para no revelar si el correo existe. |
| RN-17 | Un empleado no puede iniciar sesión si fue eliminado o si su empresa está **inactiva** o eliminada. |
| RN-18 | La sesión (token JWT) dura **8 horas**. Cada área tiene su propio token: `bbq_admin_token` y `bbq_company_token`. |
| RN-19 | En **cada** petición del panel de empresa se vuelve a comprobar que el empleado exista y que su empresa siga activa. Si la desactivan mientras alguien trabaja, pierde el acceso en su siguiente acción (401). |
| RN-20 | **Aislamiento por empresa:** un empleado solo puede ver o modificar datos de su propia empresa. Si intenta acceder a una instalación, ubicación o reserva de otra empresa, recibe un 403. |
| RN-21 | **Aislamiento por instalación:** un empleado de tipo 2 solo accede a las instalaciones que tiene asignadas en `employee_facility`. Un administrador de empresa (tipo 1) accede a todas y no necesita asignaciones. |

---

## 4. Administración de la plataforma

### 4.1 Roles

| ID | Regla |
|---|---|
| RN-22 | Un rol tiene un nombre (obligatorio, máx. 100 caracteres) y un tipo que solo puede ser 1 o 2. |
| RN-13a | No se puede eliminar un rol que tienen asignado empleados activos. |
| RN-23 | No se puede **cambiar el tipo** de un rol que ya está asignado a empleados. |
| RN-24 | Siempre debe existir **al menos un rol de tipo 1**, porque se necesita para crear empresas. No se puede eliminar el último ni cambiarle el tipo. |

### 4.2 Empresas

| ID | Regla |
|---|---|
| RN-12 | Al crear una empresa se crea también su **representante**: un empleado con el **primer rol de tipo 1** (el de menor id). Si no existe ningún rol de tipo 1, no se puede crear la empresa. |
| RN-25 | La contraseña del representante es obligatoria al crearlo (mínimo 8 caracteres). Al editar, dejarla vacía mantiene la actual. |
| RN-26 | El **representante** es el empleado de tipo 1 **más antiguo** de la empresa. Si al editar una empresa ya no tiene representante, se crea uno nuevo con los datos del formulario. |
| RN-27 | El correo de la empresa es único entre las empresas. El correo de un empleado es único **en toda la plataforma**, no solo dentro de su empresa. Los correos se guardan en minúsculas y sin espacios. |
| RN-14 | Al eliminar una empresa se hace un borrado lógico (`deleted_at`) de la empresa **y de todos sus empleados**. A sus correos se les agrega `-{id}` (`correo@x.com-15`), así el correo queda libre para registrarlo otra vez. |
| RN-28 | Una empresa **inactiva** conserva sus datos, pero sus empleados no pueden entrar (RN-17) y su portal no acepta reservas (RN-10). |

---

## 5. Panel de la empresa

### 5.1 Perfil

| ID | Regla |
|---|---|
| RN-29 | Solo el administrador de empresa (tipo 1) ve y edita el perfil: los datos de la empresa y los de su propia cuenta. |
| RN-30 | Para cambiar la contraseña hay que confirmarla (las dos deben coincidir). Si se deja vacía, se mantiene la actual. |

### 5.2 Empleados

| ID | Regla |
|---|---|
| RN-31 | Solo el administrador de empresa (tipo 1) gestiona empleados, y únicamente los de su empresa. |
| RN-32 | Al crear un empleado, la contraseña es obligatoria (mín. 8) y debe confirmarse. |
| RN-13b | Nadie puede **eliminarse a sí mismo** ni **quitarse a sí mismo** el rol de administrador. |
| RN-13c | La empresa debe tener siempre **al menos un empleado de tipo 1**: no se puede eliminar ni degradar al último. |
| RN-33 | Las instalaciones asignadas solo se guardan para empleados de tipo 2, y solo pueden ser instalaciones de la misma empresa. Si un empleado pasa a tipo 1 o se elimina, sus asignaciones se retiran. |
| RN-14b | Al eliminar un empleado: borrado lógico y su correo se libera con el sufijo `-{id}`. |

### 5.3 Instalaciones, ubicaciones y productos

| ID | Regla |
|---|---|
| RN-34 | Solo el administrador de empresa (tipo 1) crea, edita o elimina instalaciones. El empleado (tipo 2) solo ve las que tiene asignadas. |
| RN-35 | Cada instalación y cada ubicación recibe un `uuid` que se usa como enlace público del portal. |
| RN-36 | **Capacidad de la ubicación** (`max_person`): de 1 a 99 999 personas por día. **Si se deja vacía, la ubicación es exclusiva**: se acepta **una sola reserva por día**, sin importar cuántas personas vengan. |
| RN-37 | **Precio de la ubicación:** es el precio **por reserva** (no por persona). Es obligatorio, va de 0 en adelante y tiene máximo 2 decimales. |
| RN-38 | **Productos:** nombre y precio (0 o más, 2 decimales). Pertenecen a una instalación y solo se pueden vender con reservas de las ubicaciones de esa instalación. |
| RN-39 | Todo se elimina de forma lógica. Las reservas antiguas conservan el nombre y el precio del producto aunque este se elimine después. |

### 5.4 Días cerrados

| ID | Regla |
|---|---|
| RN-11 | Se registran **por ubicación**. La fecha de inicio es obligatoria. Si no se indica fecha de fin, se cierra solo ese día. La fecha de fin no puede ser anterior a la de inicio. |
| RN-11b | Los periodos cerrados de una misma ubicación **no pueden cruzarse**. |
| RN-15 | No se puede cerrar una fecha que ya tiene **reservas activas** (pendientes o pagadas). Primero hay que cancelarlas. |
| RN-02 | No se puede reservar en un día cerrado. En el calendario, esos días se muestran bloqueados. |

---

## 6. Reservas

### 6.1 Cuándo se puede reservar

| ID | Regla |
|---|---|
| RN-01 | **Ventana de reserva:** la fecha debe estar entre **hoy + 2 días** y **hoy + 60 días**, ambos incluidos. Aplica igual al portal y a la reserva manual de la empresa. "Hoy" es la fecha del servidor. |
| RN-02 | La ubicación no debe estar cerrada ese día. |
| RN-03 | **Capacidad:** personas ya reservadas ese día + personas nuevas ≤ `max_person`. En una ubicación exclusiva (`max_person` vacío), el día debe estar libre. |
| RN-03b | Solo ocupan cupo las reservas **activas**: no eliminadas y con venta **pendiente o pagada**. Las canceladas y las expiradas liberan el cupo. |
| RN-04 | El calendario marca como **llenos** los días sin cupo (o ya reservados, si la ubicación es exclusiva), además de los cerrados y los que están fuera de la ventana. |

### 6.2 Datos de la reserva

| ID | Regla |
|---|---|
| RN-40 | Son obligatorios: fecha, número de personas (1 a 99 999), nombre (máx. 100), correo válido (máx. 150) y teléfono (6 a 20 caracteres: números, espacios, `+` y `-`). |
| RN-07 | Los productos son opcionales. Deben pertenecer a la instalación de la ubicación y no estar eliminados. La cantidad va de 0 a 999; los que tienen cantidad 0 se ignoran y, si un producto viene repetido, se suman sus cantidades. |
| RN-41 | Un mismo cliente (mismo correo) puede tener varias reservas. |

### 6.3 Precios

| ID | Regla |
|---|---|
| RN-06 | Los precios **siempre los calcula el servidor**; se ignora cualquier precio que mande el navegador. |
| | `precio ubicación` = precio actual de la ubicación |
| | `total productos` = Σ (precio del producto × cantidad) |
| | `total` = precio ubicación + total productos (redondeado a 2 decimales, moneda **S/**) |
| RN-06b | Los precios se **congelan** al reservar: en `sales` y `booking_details` queda el precio de ese momento, aunque luego cambien los precios. |

### 6.4 Registro y concurrencia

| ID | Regla |
|---|---|
| RN-05 | Si dos personas reservan a la vez el último cupo, **solo una lo consigue**. Al crear la reserva se bloquea la ubicación hasta terminar de guardar (transacción `READ_COMMITTED` con bloqueo pesimista), y la disponibilidad se vuelve a validar dentro de ese bloqueo. |
| RN-42 | En el portal, el cliente primero **cotiza**: se valida todo y se muestra el resumen sin guardar nada. Al **confirmar**, se valida todo otra vez, porque el cupo pudo ocuparse mientras tanto. |

### 6.5 Estado del pago (venta)

| Estado | Código | Ocupa cupo |
|---|:-:|:-:|
| Pendiente | 1 | Sí |
| Pagado | 2 | Sí |
| Cancelado | 3 | No |
| Expirado | 4 | No |

| ID | Regla |
|---|---|
| RN-08a | **Reserva desde el portal** → queda **Pendiente** (el pago se hace por transferencia, fuera del sistema). Fecha límite de pago = la **más temprana** entre *hoy + 3 días* y *el día anterior a la reserva*. |
| RN-08b | **Reserva manual de la empresa** → queda **Pagada** de inmediato, con la fecha y hora de pago, y se registra qué empleado la creó. Las del portal aparecen como creadas por "Portal". |
| RN-09 | Transiciones permitidas: |
| | Pendiente → **Pagado** (la empresa confirma el pago; se guarda `paid_at`) |
| | Pendiente o Pagado → **Cancelado** (lo hace la empresa) |
| | Pendiente → **Expirado** (lo hace el sistema de forma automática) |
| | Cancelado y Expirado son **estados finales**: no admiten cambios. |
| RN-09b | **Vencimiento automático:** cada hora y al arrancar la aplicación, las ventas pendientes cuya fecha límite ya pasó (fecha límite **anterior a hoy**) pasan a Expirado. Se puede pagar hasta el mismo día de la fecha límite. |
| RN-09c | Cancelar o expirar **no borra** la reserva: queda en el historial y solo libera el cupo. |

```
              (empresa)               (empresa)
  Pendiente ────────────► Pagado ────────────► Cancelado
      │  │                                        ▲
      │  └────────────────────────────────────────┘ (empresa)
      │
      └──────────────────► Expirado   (sistema, al vencer el plazo)
```

### 6.6 Consulta de reservas (empresa)

| ID | Regla |
|---|---|
| RN-43 | La lista por instalación se puede filtrar por fecha de reserva, fecha de registro, código (`uuid`), estado y ubicación. Muestra **10 por página**. |
| RN-44 | Las estadísticas de la lista (total de reservas y monto; pagadas y monto; pendientes y monto) se calculan con los **mismos filtros** que la lista. |

---

## 7. Portal del cliente

| ID | Regla |
|---|---|
| RN-10 | Si la empresa está inactiva o eliminada, su portal no muestra nada ni acepta reservas (403: "Este establecimiento no está recibiendo reservas por el momento."). |
| RN-45 | El portal solo muestra instalaciones, ubicaciones y productos que no estén eliminados. |
| RN-46 | El carrito (reserva en curso) se guarda solo en el navegador del cliente (`sessionStorage`). No se guarda nada en la BD hasta que confirma. |
| RN-47 | Al confirmar, el cliente recibe un **comprobante** en `/booking-details/{uuid}`. Cualquiera que tenga ese enlace puede verlo; no requiere login. |

---

## 8. Parámetros del negocio

Están en `models/AppConstants.java` (salvo los indicados).

| Parámetro | Valor |
|---|---|
| Anticipación mínima para reservar | 2 días |
| Anticipación máxima para reservar | 60 días |
| Plazo para pagar una reserva del portal | 3 días (sin pasar del día anterior a la reserva) |
| Días llenos que se calculan para el calendario | próximos 90 días |
| Rango máximo que se consulta a la vez en el calendario | 100 días |
| Frecuencia del vencimiento automático | cada hora (`SaleExpirationJob`) |
| Duración de la sesión | 8 horas (`security/Constants.java`) |
| Reservas por página en la lista | 10 |
| Moneda | S/ |

---

## 9. Validaciones de formato comunes

| Campo | Regla |
|---|---|
| Teléfono | 6 a 20 caracteres; solo números, espacios, `+` y `-`. Obligatorio en empresa, instalación y reserva; opcional en empleados |
| Correo | Formato válido. Máx. 140 caracteres en empresas y empleados (se reserva espacio para el sufijo `-{id}` de RN-14) y 150 en instalaciones y reservas |
| URL de la empresa | Opcional; si se llena, debe empezar con `http://` o `https://` |
| Contraseña | Mínimo 8 caracteres |
| Nombres | Obligatorios, máx. 100 caracteres |
| Montos | ≥ 0, máx. 8 enteros y 2 decimales |

Los errores de validación se devuelven como **422** con el detalle por campo (`{"message", "errors": {"campo": [..]}}`).

---

## 10. Fuera del alcance y puntos a tener en cuenta

- **Sin pasarela de pago:** el pago del portal es por transferencia y la empresa lo confirma a mano (RN-09).
- **Sin franjas horarias:** una reserva ocupa el día completo de la ubicación.
- **Sin correos:** ni al cliente ni a la empresa.
- **El cliente no puede cancelar** desde el portal; tiene que comunicarse con la empresa.
- **Eliminar una instalación o ubicación no revisa las reservas futuras:** se permite aunque tenga reservas activas.
  Las reservas de una ubicación eliminada siguen apareciendo en la lista de su instalación, pero ya no en el calendario.
  Las de una instalación eliminada dejan de verse en el panel. Conviene cancelarlas antes de eliminar.
- **Sin control de stock** de productos.
