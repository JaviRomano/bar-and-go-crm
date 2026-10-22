# Fase 1.5 — Modularización con Spring Modulith

Rama: `feature/erp-fase-1-5` (desde `chore/java-25`, que a su vez parte de la Fase 1). Decisión: opción A de [modulith-evaluacion.md](modulith-evaluacion.md).

## 1. Qué se ha hecho

### Estructura

El backend pasa de paquetes por capa a paquetes por módulo. En cada módulo, el paquete raíz es su API y `internal` el resto.

| Módulo | API (paquete raíz) | `internal` | Depende de |
|---|---|---|---|
| `shared` | Excepciones, `ErrorResponse`, `GlobalExceptionHandler` | — | — |
| `identity` | `User`, `UserRole`, `StaffTag`, `CurrentUser`, `UserService`, `AuthService`, `UserDirectory`, DTOs | Implementaciones, `UserRepository`, controladores, JWT, `SecurityConfig`, usuarios demo | shared |
| `catalog` | `Product`, `ProductService`, `ProductCategoryService`, `ProductSalesCheck`, DTOs | `ProductCategory`, repositorios, implementaciones, controladores, carta demo | shared |
| `ordering` | `OrderService`, `OrderStatus`, `PaymentMethod`, DTOs | `Order`, `OrderItem`, repositorios, implementación, controlador, `OrderedProductSalesCheck` | shared, identity, catalog |
| `reservations` | `ReservationService`, `ReservationStatus`, DTOs | `Reservation`, repositorio, implementación, controlador, validación de horario | shared, identity |
| `masterdata` | `UnitOfMeasureService`, `SupplierService`, `ItemCategoryService`, `UnitConverter`, `UnitDimension`, DTOs | Entidades, repositorios, implementaciones, controladores, validación fiscal | shared |

Cada `package-info.java` declara el módulo con `@ApplicationModule` y sus dependencias permitidas (`allowedDependencies`). La clase principal lleva `@Modulithic`.

### Acoplamientos resueltos

| Antes | Ahora |
|---|---|
| `ProductServiceImpl` consultaba `OrderItemRepository` (ciclo carta <-> pedidos) | La carta define `ProductSalesCheck`; pedidos lo implementa (`OrderedProductSalesCheck`). La dependencia va solo de pedidos a carta |
| `OrderServiceImpl` usaba `ProductRepository` y comprobaba él mismo si el producto era vendible | `ProductService.getSellableProduct(id)`: la regla "existe, activo y disponible" vive en la carta |
| `OrderServiceImpl` y `ReservationServiceImpl` usaban `UserRepository` | `UserDirectory` (API de identity): `getUser(id)` y `exists(id)` |
| `DataInitializer` cargaba usuarios y carta | `DemoUserInitializer` (identity) y `DemoMenuInitializer` (catalog), cada uno independiente |
| Ciclo `dto <-> validator` | Desaparece: cada validador vive con sus DTOs |

### Entidades compartidas

`User` y `Product` están en la API de su módulo porque `Order` y `OrderItem` las referencian con relaciones JPA. Es la opción pragmática para el código existente. Los módulos ERP nuevos se relacionarán con otros módulos por id y eventos, no por relaciones JPA entre módulos.

### Dependencias

- BOM `spring-modulith-bom` 2.1.1, `spring-modulith-starter-core` y `spring-modulith-starter-test`.
- Sin registro de eventos todavía: se añadirá (migración Flyway con la tabla `event_publication`) cuando la Fase 3 o la 5 lo necesiten.

## 2. Verificación

- `./mvnw test` con JDK 25: 121 tests, 0 fallos, 0 omitidos. Son los 118 anteriores, más `ModularityTests` (2) y un test nuevo de `getSellableProduct`.
- `ModularityTests.modulesRespectTheirBoundaries` ejecuta `ApplicationModules.verify()`. Comprobado que no es un test vacío: con una dependencia temporal de `catalog.internal` a `ordering.internal`, falla señalando el ciclo y la dependencia no permitida.
- El grafo generado (`target/spring-modulith-docs/components.puml`) coincide con la tabla del apartado 1.
- Arranque real en perfil `dev` con JDK 25 contra un PostgreSQL desechable: migraciones aplicadas, carta (33 productos) y usuarios (6) cargados por sus inicializadores. Con `curl`: pedido creado (201), producto vendido archivado al borrarlo (204, deja de estar en la carta), pedido de un producto archivado rechazado (400).

## 3. Cambios de comportamiento

Ninguno en la API. Una diferencia interna: los datos demo de usuarios y de carta se cargan por separado, de modo que una BD con usuarios pero sin productos recibe la carta demo (antes se omitía todo si había usuarios).

## 4. Para las fases siguientes

- Cada fase ERP crea su módulo (`purchasing`, `inventory`, `recipes`, `waste`, `reporting`) con su `package-info.java` y sus dependencias declaradas. Si una fase necesita una dependencia no prevista, el test lo señala antes del merge.
- `ItemCategory` y `UnitOfMeasure` están hoy en `masterdata.internal`. `purchasing` (Fase 2) necesitará referenciarlas: se decidirá entonces si se exponen en la API de `masterdata` o se referencian por id.
- La Fase 5 (descuento de stock al entregar un pedido) se hará con un evento de `ordering` y un `@ApplicationModuleListener` en `inventory`, con el registro de eventos.
