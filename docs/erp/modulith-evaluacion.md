# Evaluación: migrar a Spring Modulith

Fecha: 2026-10-22. Estado: **DECISIÓN PENDIENTE**. No se ha modificado código; la prueba descrita en el apartado 2 se hizo de forma temporal y se deshizo.

## 1. Compatibilidad

| Elemento | Proyecto | Spring Modulith 2.1.1 (estable) |
|---|---|---|
| Spring Boot | 4.1.1 | Compilado contra 4.1.1 |
| Spring Framework | 7.0.x | 7.0.9 |
| Spring Data | 4.1.x | 4.1.1 |
| Java | 25 (rama `chore/java-25`) | Funciona: ArchUnit 1.4.2 analizó las clases de Java 25 sin errores |

La 2.2 está en milestone (M2) y va con Boot 4.2. Para este proyecto, 2.1.x es la versión correcta.

## 2. Qué dice Modulith del código actual

Modulith considera módulo cada subpaquete directo del paquete principal. Se ejecutó `ApplicationModules.verify()` sobre el código de la Fase 1 en JDK 25:

| Resultado | Detalle |
|---|---|
| Módulos detectados | `config`, `controller`, `dto`, `entity`, `enums`, `exception`, `repository`, `security`, `service`, `validator` |
| Violaciones | **1053**, casi todas "depende de un tipo no expuesto" (p. ej. `service` usa `dto.product.ProductDTO`, que está en un subpaquete de `dto`) |
| Ciclos | `dto -> validator -> dto` (`SupplierDTO` usa `@TaxIdValidator`; `ReservationTimeValidatorImpl` usa `ReservationRequestDTO`) |

Conclusión: **no hay migración incremental posible sobre la estructura actual.** El código está organizado por capa técnica y Modulith modela módulos de negocio. Añadir la dependencia sin reorganizar solo produce ruido. La migración es, ante todo, pasar de *package-by-layer* a *package-by-module*.

## 3. Módulos propuestos

```
com.barandgo.crm
├── shared        excepciones, ErrorResponse, GlobalExceptionHandler, validación fiscal
├── identity      User, roles, etiquetas, auth, JWT, CurrentUser, SecurityConfig
├── catalog       Product, ProductCategory (carta)
├── ordering      Order, OrderItem                  -> catalog, identity
├── reservations  Reservation                       -> identity
├── masterdata    unidades, conversiones, proveedores, categorías de compra
│   (Fase 2) purchasing   artículos, pedidos a proveedor        -> masterdata
│   (Fase 3) inventory    almacenes, ledger, PMP, recuentos      -> masterdata, purchasing
│   (Fase 4) recipes      escandallos, producción                -> catalog, masterdata, inventory
│   (Fase 5) waste        mermas                                 -> inventory, recipes
│   (Fase 6) reporting    informes (solo lectura)
```

Cada módulo expone en su paquete raíz lo que otros pueden usar (servicio, DTOs) y deja el resto en `internal`. El nombre de las tablas no cambia: todas las entidades llevan `@Table` explícito, así que mover paquetes no requiere migraciones de esquema.

### Acoplamientos actuales que hay que resolver

| Acoplamiento | Problema | Solución |
|---|---|---|
| `ProductServiceImpl` usa `OrderItemRepository` para saber si un producto tiene ventas | Ciclo `catalog <-> ordering` (ordering ya depende de catalog) | SPI en catalog (`ProductUsage`) que implementa ordering. La dependencia queda en un solo sentido |
| `OrderItem -> Product` y `Order -> User` como relaciones JPA | Ordering usa entidades de otros módulos | A corto plazo, exponer `Product` y `User` en la API de su módulo. Para los módulos ERP nuevos, referencias por id entre módulos |
| `DataInitializer` escribe usuarios, productos y categorías | Toca tres módulos | Un inicializador por módulo, o mantenerlo en un paquete de arranque de `dev` fuera de los módulos |
| `SecurityConfig` conoce las rutas de todos los módulos | Configuración central | Aceptable en `identity`. Alternativa: que cada módulo aporte sus reglas con un bean `Customizer` |
| `dto <-> validator` | Ciclo | Desaparece al repartir DTOs y validadores por módulo |

## 4. Qué aporta para la iniciativa ERP

- **Fase 5 desacoplada.** "Al pasar un pedido a ENTREGADO, descontar stock" es el caso para el que está pensado Modulith: `ordering` publica `OrderDelivered` y `inventory` lo procesa con `@ApplicationModuleListener`. Ordering no necesita conocer el inventario. La cancelación publica `OrderCancelled` para la compensación.
- **Event Publication Registry.** El evento se guarda en la misma transacción que el cambio de estado del pedido (patrón *outbox*). Si el listener falla, la publicación queda pendiente y se puede reintentar; no se pierde un descuento de stock por un error puntual.
- **Verificación continua.** Un test con `verify()` impide que una fase cree dependencias no previstas. Los tests `@ApplicationModuleTest` arrancan un módulo con sus dependencias, no la aplicación entera.
- **Documentación generada.** `Documenter` produce diagramas C4/PlantUML de módulos y dependencias, útiles para el README del portfolio.

## 5. Implicaciones operativas

| Tema | Detalle |
|---|---|
| Dependencias | BOM `spring-modulith-bom` 2.1.1; `spring-modulith-starter-core`, `spring-modulith-starter-jdbc` (registro de eventos) y `spring-modulith-starter-test` |
| Registro de eventos con Flyway | Con `ddl-auto=validate`, la tabla la crea Flyway (V9) con el DDL oficial para PostgreSQL (`event_publication`: id UUID, listener_id, event_type, serialized_event, publication_date, completion_date, status, completion_attempts, last_resubmission_date). Se desactiva la creación automática: `spring.modulith.events.jdbc.schema-initialization.enabled=false` |
| JDBC frente a JPA para el registro | Recomiendo JDBC. Con el starter JPA, Hibernate validaría su entidad de publicación contra la tabla; con JDBC solo hay SQL, sin entidad que desalinear |
| Entrega al menos una vez | Un evento puede llegar dos veces (reintento, reinicio). Los listeners tienen que ser idempotentes: ya está previsto con `order_stock_posting` y el índice único de movimientos (Fase 0, 3.2) |
| Consistencia | `@ApplicationModuleListener` es asíncrono y se ejecuta tras el commit en otra transacción. El stock se descuenta un instante después de marcar el pedido como entregado, no en la misma transacción. Para este dominio es aceptable; si no lo fuera, se usa un listener síncrono y se pierde parte del desacoplamiento |
| Hilos | Los listeners asíncronos usan el executor de Spring. Con Java 25 se pueden activar hilos virtuales (`spring.threads.virtual.enabled=true`) |
| Pendientes y limpieza | Configurar el reenvío de publicaciones incompletas al reiniciar y una purga periódica de las completadas, para que la tabla no crezca sin límite |
| Despliegue | Sigue siendo un solo jar y una sola BD. No cambia el docker-compose ni el frontend |

## 6. Coste y momento

- **Trabajo:** mover 96 clases de producción y 13 de test a paquetes por módulo (refactor mecánico con el IDE), resolver los acoplamientos del apartado 3, añadir el test de verificación y los `package-info.java` con `@ApplicationModule`. No cambia la API REST, el esquema ni el frontend. Estimación: una fase propia, de uno a dos días.
- **Momento:** ahora, entre la Fase 1 y la Fase 2. Cada fase ERP añade unas 30-40 clases a la estructura por capas; hacerlo después multiplica el refactor. Hacerlo en paralelo a una fase ERP genera conflictos en casi todos los ficheros.

## 7. Alternativas

| Opción | Coste | Resultado |
|---|---|---|
| **A. Migrar a Modulith ahora** (recomendada) | Una fase de refactor | Módulos verificados, eventos con outbox para las fases 3-5, diagramas generados |
| B. Solo reglas de ArchUnit por capas | Bajo | Impide dependencias entre capas, pero no modela el negocio ni aporta eventos |
| C. Modulith solo para los módulos ERP nuevos (`detection-strategy=explicitly-annotated`) | Bajo ahora | Dos estilos de organización en el mismo proyecto. Los módulos existentes quedan sin verificar. Mala señal en un repo de portfolio |

## 8. Recomendación

Opción A, como **Fase 1.5 — Modularización**, antes de la Fase 2, en `feature/erp-fase-1-5`. Orden propuesto:

1. Integrar `chore/java-25` (la base de la reestructuración debe ser la definitiva).
2. Reorganizar paquetes sin cambiar comportamiento; la suite actual (118 tests) debe seguir pasando sin modificar sus aserciones.
3. Resolver los acoplamientos del apartado 3 y dejar `verify()` en verde.
4. Añadir el registro de eventos (V9) solo cuando la Fase 3 o la 5 lo necesiten, no antes.

## Fuentes

- [Spring Modulith 2.2 M1, 2.1.1, 2.0.8 y 1.4.13 (blog de Spring)](https://spring.io/blog/2026/08/26/spring-modulith-2-2-m1-2-1-1-2-0-8-and-1-4-13-released/)
- [Spring Modulith 2.1 M1, 2.0.2 y 1.4.7 (blog de Spring)](https://spring.io/blog/2026/01/23/spring-modulith-2-1-m-1-2-0-2-and-1-4-7-released/)
- [Referencia: fundamentos](https://docs.spring.io/spring-modulith/reference/fundamentals.html)
- [Referencia: apéndice (esquema del registro de eventos y propiedades)](https://docs.spring.io/spring-modulith/reference/appendix.html)
- POM de `spring-modulith-core` 2.1.1 en Maven Central (versiones de Boot, Framework y ArchUnit)
