# Bar&Go CRM

Aplicación de gestión para un bar-restaurante: carta, pedidos para recoger, reservas de mesa, clientes y personal, con una ampliación ERP en curso. Monorepo con una API REST en Spring Boot y un cliente web en React.

| Capa | Tecnología |
|---|---|
| Backend | Java 25, Spring Boot 4.1, Spring Modulith 2.1, Spring Security (OAuth2 Resource Server, JWT HS256), Spring Data JPA, Flyway |
| Base de datos | PostgreSQL 17 (docker-compose) |
| Frontend | React 19, Vite 7, React Router 7 |
| Tests | JUnit 5, Mockito, MockMvc, Testcontainers |

```
bar-and-go-crm/
├── backend/             API REST (Maven)
├── frontend/            Cliente web (Vite)
├── docker-compose.yml   PostgreSQL para desarrollo
└── .env.example         Variables de entorno
```

### Módulos del backend

El backend es un monolito modular ([Spring Modulith](https://spring.io/projects/spring-modulith)): un paquete por área de negocio, con su API en la raíz del paquete y la implementación en `internal`. Un test (`ModularityTests`) falla si un módulo usa tipos internos de otro, crea un ciclo o depende de un módulo no declarado.

| Módulo | Contenido | Depende de |
|---|---|---|
| `shared` | Excepciones y su traducción a respuestas HTTP | — |
| `identity` | Usuarios, roles, JWT, reglas de acceso | shared |
| `catalog` | Carta: productos, categorías, IVA de venta y alérgenos | shared, masterdata |
| `ordering` | Pedidos para recoger | shared, identity, catalog |
| `reservations` | Reservas de mesa | shared, identity |
| `masterdata` | Unidades de medida, proveedores, categorías de compra, tipos de IVA, alérgenos | shared |
| `purchasing` | Artículos de compra, precios por proveedor, pedidos a proveedor | shared, identity, masterdata |
| `inventory` | Almacenes, movimientos de stock (inmutables), PMP, lotes, recepción de albaranes, recuentos, reposición | shared, identity, masterdata, purchasing |
| `recipes` | Escandallos, sub-escandallos, food cost, alérgenos de cada plato, producción de elaboraciones | shared, identity, masterdata, purchasing, catalog, inventory |
| `costcontrol` | Descuento de stock por venta (escucha los eventos de pedidos), mermas, consumo teórico frente a real | shared, identity, masterdata, purchasing, catalog, ordering, recipes, inventory |
| `reporting` | Informes: valor de stock, food cost real frente a teórico, mermas, rotación, precios de compra, ingeniería de menú. Solo lectura | todos los anteriores salvo reservations |

`./mvnw test` genera además los diagramas de módulos (PlantUML/C4) en `backend/target/spring-modulith-docs`.

Documentación adicional: [guía de arranque](docs/GUIA-ARRANQUE.md) (paso a paso en bash y PowerShell, despliegue de prueba, acceso remoto por túnel y problemas frecuentes), [registro de cambios y motivos](docs/CAMBIOS.md) y [documentación de la ampliación ERP](docs/erp/).

## Ampliación ERP

El proyecto ha crecido de CRM a back-office de restauración: compras, inventario, escandallos, mermas e informes de coste, con su sección «Gestión» en el panel. Se ha desarrollado por fases, cada una documentada en [docs/erp](docs/erp/).

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Auditoría, modelo de dominio y decisiones | Hecha |
| 1 | Flyway, roles de encargado y personal, unidades de medida, proveedores, categorías | Hecha |
| 1.5 | Monolito modular con Spring Modulith | Hecha |
| 2 | Artículos de compra, tipos de IVA, precios por proveedor, pedidos a proveedor | Hecha |
| 3 | Almacenes, movimientos de stock, valoración PMP, recepción de albaranes, recuentos | Hecha |
| 4 | Escandallos y producción | Hecha |
| 5 | Mermas, descuento de stock por venta, consumo teórico frente a real | Hecha |
| 6 | Informes y pantallas de gestión | Hecha |

## Puesta en marcha

Requisitos: JDK 25, Node 20.19+ o 22.12+ (requisito de Vite 7), Docker.

```bash
cp .env.example .env
# Rellenar DB_PASSWORD y JWT_SECRET (openssl rand -base64 32)
# Si ya hay un PostgreSQL local en 5432, poner DB_PORT=5433

docker compose up -d

cd backend
./mvnw spring-boot:run        # http://localhost:8080

cd ../frontend
npm install
npm run dev                   # http://localhost:5173
```

El backend lee el `.env` de la raíz (o de `backend/`). Las variables del sistema tienen prioridad, que es lo que se usa en despliegue.

### Perfiles

| Perfil | Uso | Esquema | Datos de demo |
|---|---|---|---|
| `dev` (por defecto) | Desarrollo local | Flyway + `validate` | Sí, si la BD está vacía |
| `prod` | Despliegue (`SPRING_PROFILES_ACTIVE=prod`) | Flyway + `validate` | No |
| `test` | Suite de tests | Flyway + `validate` sobre contenedor efímero | No |

El esquema lo crean las migraciones de `backend/src/main/resources/db/migration` al arrancar; Hibernate solo comprueba que las entidades coinciden con él. Una BD creada antes de Flyway (con el antiguo `ddl-auto=update`) se adopta como versión 1 y recibe solo las migraciones posteriores.

### Despliegue

```bash
# Backend: jar ejecutable
cd backend
./mvnw clean package
SPRING_PROFILES_ACTIVE=prod \
CORS_ALLOWED_ORIGINS=https://tu-dominio \
java -jar target/crm-backend-0.0.1-SNAPSHOT.jar

# Frontend: estáticos en dist/, servibles por cualquier servidor web con fallback a index.html
cd frontend
VITE_API_BASE_URL=https://tu-dominio/api npm run build
```

El esquema se crea o actualiza al arrancar, también en `prod`.

### Acceso remoto para pruebas

Para probar la aplicación desde otro dispositivo por internet sin desplegarla, se publica el servidor de preview de Vite con un túnel (Cloudflare Quick Tunnel o ngrok). Vite reenvía `/api` al backend, así que basta un solo túnel y el backend no se expone:

```
navegador remoto --HTTPS--> túnel --> vite preview :4173 --/api--> backend :8080
```

Requiere compilar el frontend con `VITE_API_BASE_URL=/api` y arrancar el preview con `VITE_ALLOWED_HOSTS=.trycloudflare.com`. Los pasos completos están en el [apartado 8 de la guía de arranque](docs/GUIA-ARRANQUE.md#8-acceso-desde-otro-equipo-por-internet). La URL del túnel es pública mientras esté activo, y las credenciales de demo también lo son.

### Credenciales de demo

Solo existen con el perfil `dev`. Son públicas a propósito para poder probar la aplicación:

| Rol | Email | Contraseña |
|---|---|---|
| ADMIN | admin@bar.com | admin123 |
| MANAGER | encargado@bar.com | encargado123 |
| STAFF | personal@bar.com | personal123 |
| CUSTOMER | ripleynostromo@example.com | cliente123 |

Se pueden cambiar con `DEMO_ADMIN_PASSWORD`, `DEMO_MANAGER_PASSWORD`, `DEMO_STAFF_PASSWORD` y `DEMO_CUSTOMER_PASSWORD`.

Con `dev` se cargan también datos demo del ERP: proveedores, 13 artículos de compra con tarifa, 4 almacenes y stock con lotes procedente de tres pedidos recibidos (uno en parte), y escandallos de la carta demo con una salsa elaborada en stock. Detalle en [docs/erp/03-inventario.md](docs/erp/03-inventario.md) y [docs/erp/04-escandallos.md](docs/erp/04-escandallos.md).

## Autenticación

`POST /api/auth/login` devuelve un token de acceso:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 7200,
  "user": { "id": 1, "name": "Admin Demo", "email": "admin@bar.com", "role": "ADMIN" }
}
```

El resto de peticiones lo envían en la cabecera `Authorization: Bearer <token>`. El token lleva el email (`sub`), el id (`uid`) y el rol (`role`). Está firmado con HS256 y caduca según `JWT_EXPIRATION` (2 horas por defecto). No hay refresh token: al caducar, la API responde 401 y hay que volver a iniciar sesión.

El registro público (`POST /api/auth/register`) siempre crea usuarios `CUSTOMER`; si el cuerpo incluye un rol, se ignora. El personal se da de alta desde `/api/users`.

### Roles

| Rol | Quién | Puede |
|---|---|---|
| `ADMIN` | Administración | Todo, incluida la configuración (unidades de medida) y la gestión de encargados |
| `MANAGER` | Encargado | Lo mismo que STAFF, más carta, proveedores, compras, ajustes, costes, informes, cancelación de pedidos entregados, borrado de pedidos y reservas, y alta de personal y clientes |
| `STAFF` | Trabajador | Panel de pedidos y reservas; consulta de proveedores, categorías de compra, artículos y stock (sin precios ni costes); recepción de mercancía, traspasos y recuentos; escandallos sin costes y producción de elaboraciones; registro de mermas |
| `CUSTOMER` | Cliente | Carta, sus pedidos y sus reservas |

Los permisos son jerárquicos: ADMIN incluye los de MANAGER y MANAGER los de STAFF. El personal puede llevar etiquetas de puesto (`KITCHEN`, `FLOOR`, `BAR`, `DISHWASHING`), que son informativas y no dan permisos. Las rutas sin regla explícita se deniegan.

## API

Leyenda de acceso: **público** (sin token), **auth** (cualquier usuario autenticado), **STAFF**, **MANAGER**, **ADMIN**. Por la jerarquía, un acceso STAFF también lo tienen MANAGER y ADMIN.

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | público | Inicio de sesión |
| POST | `/api/auth/register` | público | Alta de cliente |
| GET | `/api/auth/me` | auth | Usuario autenticado |
| GET | `/api/products`, `/available`, `/{id}`, `/category/{c}` | público | Carta, con IVA y alérgenos de cada producto |
| POST, PUT, PATCH, DELETE | `/api/products/**` | MANAGER | Gestión de la carta. `DELETE` archiva el producto si ya tiene ventas |
| GET | `/api/product-categories?includeInactive=` | público | Categorías de la carta, en orden |
| POST, PUT, DELETE | `/api/product-categories/**` | MANAGER | Gestión de categorías. No se borra una categoría con productos |
| POST | `/api/orders` | auth | Crear pedido para el usuario del token |
| GET | `/api/orders/me?status=` | auth | Pedidos propios, con filtro opcional por estado |
| GET | `/api/orders/{id}` | auth | Pedido propio (personal: cualquiera) |
| GET | `/api/orders?from=&to=` | STAFF | Todos, u opcionalmente por intervalo (ISO-8601) |
| GET | `/api/orders/upcoming` | STAFF | Pedidos con recogida futura, por hora |
| GET | `/api/orders/status/{s}`, `/user/{id}` | STAFF | Filtros |
| PATCH | `/api/orders/{id}/status` | STAFF | Cambiar estado |
| DELETE | `/api/orders/{id}` | MANAGER | Eliminar |
| POST | `/api/reservations` | auth | Crear reserva |
| GET | `/api/reservations/me` | auth | Reservas propias (asociadas por teléfono) |
| PATCH | `/api/reservations/{id}/cancel` | auth | Cancelar reserva propia (personal: cualquiera) |
| GET | `/api/reservations?from=&to=` | STAFF | Todas, u opcionalmente por intervalo de fechas |
| GET | `/api/reservations/upcoming?status=` | STAFF | Desde hoy, por estado (por defecto `CONFIRMED`) |
| GET | `/api/reservations/{id}`, `/date/{d}`, `/status/{s}` | STAFF | Consultas |
| PUT | `/api/reservations/{id}` | STAFF | Modificar o confirmar |
| DELETE | `/api/reservations/{id}` | MANAGER | Eliminar |
| * | `/api/users/**` | MANAGER | Gestión de usuarios. Un MANAGER solo gestiona STAFF y CUSTOMER |
| GET | `/api/suppliers?active=`, `/{id}` | STAFF | Proveedores |
| POST, PUT | `/api/suppliers/**` | MANAGER | Alta y modificación. Sin borrado: se desactivan con `active=false` |
| GET | `/api/item-categories` | STAFF | Categorías de artículos de compra |
| POST, PUT, DELETE | `/api/item-categories/**` | MANAGER | Gestión de esas categorías |
| GET | `/api/units`, `/conversions`, `/convert?quantity=&from=&to=` | MANAGER | Unidades de medida, conversiones y conversión de cantidades |
| POST, DELETE | `/api/units/**` | ADMIN | Alta de unidades y conversiones genéricas |
| GET | `/api/vat-rates`, `/{id}` | MANAGER | Tipos de IVA |
| POST, PUT | `/api/vat-rates/**` | ADMIN | Alta y modificación. Sin borrado: se desactivan |
| GET | `/api/stock-items?active=&categoryId=`, `/{id}` | STAFF | Artículos de compra |
| POST, PUT | `/api/stock-items/**` | MANAGER | Alta y modificación, con alérgenos y conversiones propias (1 caja = 24 ud) |
| GET, PUT, DELETE | `/api/stock-items/{id}/suppliers/{supplierId}` | MANAGER | Precio del artículo en cada proveedor |
| GET | `/api/stock-items/{id}/price-history` | MANAGER | Precios pagados en cada compra |
| GET | `/api/purchase-orders?status=&supplierId=`, `/{id}` | MANAGER | Pedidos a proveedor, con base e IVA por tipo |
| POST, PUT, DELETE | `/api/purchase-orders/**` | MANAGER | Alta y edición de borradores; `/{id}/send`, `/{id}/cancel` y `/{id}/close` (recibido con faltas) |
| GET | `/api/warehouses` | STAFF | Almacenes |
| POST, PUT | `/api/warehouses/**` | MANAGER | Alta y modificación. Sin borrado: se desactivan |
| GET | `/api/inventory/stock?warehouseId=&stockItemId=` | STAFF | Stock por artículo y almacén, sin costes |
| GET | `/api/inventory/lots/expiring?days=` | STAFF | Lotes que caducan en los próximos días |
| POST | `/api/inventory/transfers` | STAFF | Traspaso entre almacenes; los lotes viajan con la mercancía |
| GET | `/api/inventory/valuation`, `/movements?stockItemId=&warehouseId=&from=&to=` | MANAGER | Valoración PMP y libro de movimientos |
| POST | `/api/inventory/adjustments` | MANAGER | Ajuste manual con motivo |
| GET, POST | `/api/inventory/replenishment`, `/replenishment/orders` | MANAGER | Sugerencia de reposición y pedidos en borrador a partir de ella |
| POST | `/api/goods-receipts` | STAFF | Recepción de albarán: entradas de stock, lotes, pedido y precio de compra. El personal no puede cambiar precios |
| GET | `/api/goods-receipts?supplierId=`, `/{id}` | MANAGER | Albaranes recibidos |
| POST | `/api/stock-counts`, `/{id}/close` | MANAGER | Abrir y cerrar recuentos; al cerrar se ajustan las diferencias |
| GET, PUT | `/api/stock-counts`, `/{id}`, `/{id}/lines` | STAFF | Consultar y registrar cantidades contadas (sin ver el teórico) |
| GET | `/api/recipes?type=`, `/{id}` | STAFF | Escandallos con coste, food cost, margen, PVP sugerido y alérgenos (sin costes para el personal) |
| POST, PUT | `/api/recipes/**` | MANAGER | Alta y edición; se rechazan ciclos de sub-escandallos |
| GET, POST | `/api/recipes/{id}/cost-history`, `/recalculate` | MANAGER | Histórico de coste y recálculo completo |
| POST | `/api/recipes/{id}/production` | STAFF | Producir una elaboración: consume ingredientes y da de alta el resultado al coste real |
| POST | `/api/waste` | STAFF | Registrar una merma de un artículo (en cualquier unidad convertible) o de un plato (por raciones, según su escandallo) |
| GET | `/api/waste?from=&to=&reason=` | MANAGER | Mermas valoradas al PMP |
| GET | `/api/cost-control/orders/{id}/stock-posting` | MANAGER | Descuento de stock de un pedido entregado y su anulación |
| GET | `/api/cost-control/consumption?fromCountId=&toCountId=` | MANAGER | Consumo teórico frente a real entre dos recuentos cerrados |
| GET | `/api/reports/stock-value?at=&warehouseId=` | MANAGER | Valor contable del stock a una fecha, por artículo, categoría y almacén |
| GET | `/api/reports/food-cost?from=&to=&warehouseId=` | MANAGER | Food cost real frente a teórico, con desglose del coste real y serie por día o semana |
| GET | `/api/reports/waste?from=&to=&warehouseId=` | MANAGER | Mermas por motivo, por lo registrado y por artículo |
| GET | `/api/reports/rotation?from=&to=&warehouseId=` | MANAGER | Rotación y días de cobertura por artículo |
| GET | `/api/reports/supplier-prices?stockItemId=&supplierId=&from=&to=` | MANAGER | Evolución del precio pagado por unidad de inventario |
| GET | `/api/reports/menu-engineering?from=&to=` | MANAGER | Matriz popularidad × margen (estrella, vaca, puzzle, perro) |

Los errores tienen siempre la misma forma:

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "/api/orders", "validationErrors": {} }
```

### Pedidos

El cliente envía producto, cantidad y método de pago (`CARD` o `CASH`, se paga en la recogida):

```json
{ "timeTakeAway": "2026-10-01T14:30:00", "paymentMethod": "CARD", "items": [ { "productId": 1, "quantity": 2 } ] }
```

Cada línea guarda el producto (`productId`) y, además, una copia de su nombre y precio en el momento del pedido. Así el cliente no puede fijar el precio, y los cambios posteriores de la carta no alteran los pedidos pasados. No se aceptan productos no disponibles ni archivados.

Estados: `CREATED` → `IN_PREPARATION` → `READY` → `DELIVERED`, o `CANCELLED` desde cualquiera de ellos. No se pueden saltar pasos (409). Al entregar, el pedido descuenta del almacén de ventas lo que dicen los escandallos de sus platos; solo un encargado puede cancelar un pedido ya entregado, y entonces el stock vuelve. Un pedido entregado no se borra.

### Reservas

- Horario: de 12:00 a 16:00 y de 20:00 a 23:00. Los martes el local está cerrado.
- Entre 1 y 50 personas, siempre con fecha futura.
- Estas reglas se aplican tanto al crear como al modificar una reserva.
- Estados: `PENDING`, `CONFIRMED`, `CANCELLED`.

## Tests

```bash
cd backend
./mvnw verify
```

| Tipo | Qué cubre |
|---|---|
| Unitarios (Mockito y JUnit) | Servicios de pedidos, reservas, autenticación, usuarios, productos y unidades; conversión de unidades; validación de CIF/NIF/NIE; horario de reservas; tokens |
| Slice web (`@WebMvcTest`) | Reglas de autorización por rol y ruta, jerarquía de roles; CORS |
| Integración (Testcontainers) | Flujos completos contra PostgreSQL con el esquema de Flyway: pedidos, roles, proveedores, archivado de productos, bloqueo optimista, compras (artículo, tarifa, pedido a proveedor y su ciclo de vida), inventario (recepción, PMP, lotes, traspasos, recuento, reposición, inmutabilidad del ledger), escandallos (coste, food cost, alérgenos, ciclos, producción, recálculo por evento), control de costes (descuento por venta idempotente, anulación, mermas, consumo frente a dos recuentos), informes con importes calculados a mano |
| Migraciones (Testcontainers + Flyway) | Migración de datos existentes (categorías, enlace pedido-producto) y adopción de una BD anterior a Flyway |
| Modularidad (Spring Modulith) | Fronteras entre módulos y dependencias declaradas; genera la documentación de módulos |

Los tests de integración necesitan Docker; si no está disponible, se omiten y el resto de la suite se ejecuta igual.

## Decisiones técnicas

- **JWT con el resource server de Spring Security** en lugar de un filtro propio con jjwt. La validación de firma, caducidad y emisor la hace el framework (Nimbus), así que el código propio se limita a emitir el token y a traducir el claim `role` a autoridades.
- **HS256 con clave simétrica**: es suficiente mientras una sola aplicación emite y valida los tokens. Si hubiera más servicios, convendría pasar a RS256 con clave pública.
- **Spring Boot 4.1**: la línea 3.x ya no tiene soporte OSS.
- **DTOs existentes como clases con constructor vacío, getters y setters**, porque los servicios los rellenan campo a campo (`dto.setX(...)`). Los DTOs añadidos con la ampliación ERP (categorías, unidades, proveedores) son `record`: se construyen de una vez desde la entidad (`from(...)`), son inmutables, y Jackson y Bean Validation los admiten sin configuración.
- **Flyway con `ddl-auto=validate`** en todos los perfiles: el esquema está versionado y revisable, y Hibernate detecta al arrancar cualquier entidad que no coincida con él.
- **Monolito modular con Spring Modulith** en lugar de paquetes por capa (`controller`, `service`, `entity`...). Cada área de negocio es un módulo con API explícita y dependencias verificadas en los tests. Prepara la ampliación ERP (compras, inventario, escandallos) sin que los módulos nuevos se acoplen a los internos de los existentes. Cuando un módulo necesita algo de otro que dependería de él, se invierte la dependencia: la carta pregunta si un producto tiene ventas a través de `ProductSalesCheck`, que implementa el módulo de pedidos.
- **Bloqueo optimista (`@Version`)** en las entidades que se editan desde el panel: si dos personas modifican el mismo registro, la segunda recibe un 409 en vez de pisar el cambio.
- **Enums en inglés**, con las etiquetas visibles resueltas en el frontend.
- **Sistema de diseño con tokens CSS** (`frontend/src/styles/tokens.css`): colores, tipografía (Sora e Inter), espaciado y tamaños táctiles. Las pantallas de Gestión solo usan tokens y se cargan bajo demanda (`React.lazy`), así que el panel de sala y la parte de cliente no cargan su código.

## Limitaciones conocidas y trabajo pendiente

- **Reservas asociadas por teléfono**: una reserva no tiene relación directa con `User`. `/api/reservations/me` compara con el teléfono del usuario, de modo que un teléfono escrito con otro formato no se asocia. Si el proyecto crece hacia ERP/CRM, lo lógico es añadir la relación `Reservation → User`.
- **BD local anterior a los cambios de esquema** (enums en inglés, `order_items`, `payment_method`): hay que recrearla (`docker compose down -v`). Flyway adopta como versión 1 una BD con el esquema de `V1__baseline.sql`, no una con el esquema antiguo en español.
- **Token en `localStorage`**: queda expuesto ante un XSS. La alternativa más robusta es una cookie `HttpOnly` emitida por el backend.

## Licencia

MIT. Ver [LICENSE](LICENSE).
