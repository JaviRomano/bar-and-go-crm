# Fase 1 — Cimientos

Rama: `feature/erp-fase-1` (desde `feature/erp-fase-0`). Decisiones de la Fase 0 aplicadas: todas las recomendaciones de `00-auditoria.md`, confirmadas en bloque.

## 1. Qué se ha hecho

### Esquema versionado

- Flyway 12.4 (`spring-boot-starter-flyway` + `flyway-database-postgresql`). `ddl-auto=validate` en todos los perfiles; ningún perfil genera esquema con Hibernate.
- `baseline-on-migrate=true` con `baseline-version=1`: una BD creada antes por `ddl-auto=update` se adopta como V1 y recibe solo V2 en adelante.

| Migración | Contenido |
|---|---|
| `V1__baseline` | Esquema previo: `users`, `products`, `orders`, `order_items`, `reservations`, equivalente al que generaba Hibernate |
| `V2__user_roles_and_tags` | Roles `STAFF` y `MANAGER` en el CHECK de `users.role`; tabla `user_tag` |
| `V3__optimistic_locking` | Columna `version` en las cuatro entidades editables |
| `V4__product_categories` | Tabla `product_category`, migración de `products.category` (texto) a `category_id` (FK), columna `products.active` |
| `V5__item_categories` | Tabla `item_category` (sin uso hasta la Fase 2) |
| `V6__units_of_measure` | `unit_of_measure` y `unit_conversion` con datos iniciales (kg, g, l, cl, ml, ud, doc, caja) |
| `V7__suppliers` | Tabla `supplier` |
| `V8__order_item_product_link` | `order_items.product_id` (FK nullable), enlace de datos existentes y tabla `order_item_link_issue` |

Migración de datos:

- **Categorías (V4).** Se siembran las cuatro categorías de la carta. Cualquier otro valor de texto ya presente en `products` se conserva como categoría propia (recortando espacios). Los productos sin categoría van a `SIN CATEGORIA`, que solo se crea si hace falta.
- **Enlace pedido-producto (V8).** Solo se enlaza por coincidencia exacta de nombre con un único producto, porque `products.name` no es único. Las líneas restantes quedan en `order_item_link_issue` con motivo `AMBIGUOUS` (varios productos con ese nombre) o `NOT_FOUND`. El snapshot `product_name` y `unit_price` no se toca.

### Unidades y conversión

- `UnitConverter` (`service/conversion`): conversor puro, sin Spring ni JPA. Usa cada regla en los dos sentidos, encadena reglas (caja → ud → doc) y permite que las reglas de un artículo sustituyan a las genéricas para el mismo par (`withItemRules`).
- Precisión: el camino se acumula como fracción y se divide una sola vez al final con `MathContext.DECIMAL128`, para que las cadenas largas no acumulen redondeo.
- Las conversiones genéricas solo se admiten dentro de la misma magnitud (`MASS`, `VOLUME`, `COUNT`). Entre magnitudes (1 botella = 0,75 l) la equivalencia depende del artículo y llega con la Fase 2.
- Se rechaza una regla nueva si ya existe un camino entre esas unidades: o sobra o contradice a las existentes.

### Proveedores

- `Supplier`: CIF/NIF, razón social, nombre comercial, contacto, email, teléfono, condiciones de pago (texto), plazo de entrega en días, notas, activo, fecha de alta.
- Validación del identificador fiscal (`TaxIdRules`): DNI, NIE, NIF especiales K/L/M y NIF de persona jurídica con su dígito de control; prefijo `ES` admitido; NIF-IVA de otros países de la UE solo por formato. Se guarda normalizado (mayúsculas, sin espacios, guiones ni puntos) y es único.
- Sin borrado: un proveedor se desactiva.

### Roles y seguridad

- `UserRole`: `CUSTOMER`, `STAFF`, `MANAGER`, `ADMIN`. Jerarquía `ADMIN > MANAGER > STAFF` con `RoleHierarchy`.
- Etiquetas de puesto (`StaffTag`: `KITCHEN`, `FLOOR`, `BAR`, `DISHWASHING`) en `user_tag`. Informativas; un cliente no puede tenerlas.
- `SecurityConfig`: reglas para los recursos nuevos y **`anyRequest().denyAll()`**. Una ruta sin regla devuelve 403 incluso a ADMIN, así que los endpoints de las fases siguientes no quedan abiertos por olvido.
- `UserServiceImpl`: un MANAGER solo crea, modifica o borra usuarios STAFF y CUSTOMER, y no puede ascender a nadie a MANAGER o ADMIN. El registro público pasa por `registerCustomer`, que fuerza `CUSTOMER` y descarta etiquetas.
- `CurrentUser.isAdmin()` sustituido por `isBackOffice()`: cualquier miembro del personal ve pedidos y reservas de cualquier cliente.

### Concurrencia

`@Version` en `User`, `Product`, `Order`, `Reservation` y en todas las entidades nuevas. `ObjectOptimisticLockingFailureException` se traduce a 409 con un mensaje para recargar.

### Productos

- `Product.category` pasa a ser `ProductCategory` (FK). La API recibe `categoryId` y devuelve `categoryId` y `category` (el nombre), de modo que los filtros por nombre del frontend siguen funcionando.
- Baja lógica (`active`): `DELETE` de un producto con ventas lo archiva (`active=false`, `available=false`); sin ventas, lo borra. Los archivados desaparecen de la carta y de los listados, siguen accesibles por id y no pueden volver a estar disponibles.

### Frontend

- `ProtectedRoute` acepta una lista de roles (`allowedRoles`). La redirección por rol se centraliza en `homePathFor`.
- Panel de gestión para STAFF, MANAGER y ADMIN. Productos solo para MANAGER y ADMIN. El botón de eliminar reserva solo aparece a quien puede usarlo.
- Las categorías de carta salen de `/api/product-categories` en el formulario de producto, la lista de productos y la carta del cliente.
- Login: accesos rápidos de demo para encargado y personal.

## 2. Desviaciones respecto a la Fase 0

| Tema | En `00-auditoria.md` | Implementado | Motivo |
|---|---|---|---|
| D7, borrar producto con ventas | 409 | Se archiva y responde 204 | El panel existente no cambia y el resultado es el que quiere quien pulsa "Eliminar": que el producto deje la carta. No se pierde trazabilidad |
| D3, autorización | `@PreAuthorize` por método | Reglas por ruta en `SecurityConfig` + `denyAll` por defecto + reglas de rol en servicio donde dependen del dato | Es el patrón que ya usa el proyecto y está cubierto por `SecurityRulesTest`. Mezclar los dos estilos dispersa las reglas |
| DTOs | No se trató | `record` en los DTOs nuevos | Documentado en el README; los DTOs existentes no cambian |

## 3. Verificación

- `./mvnw test`: 118 tests, 0 fallos, 0 omitidos (Docker activo). Incluye `MigrationDataTest`, que lleva una BD a V3 o V7, carga datos antiguos y comprueba el resultado de V4 y V8, y la adopción de una BD sin historial de Flyway.
- `npm run lint` y `npm run build`: sin errores.
- Arranque real en perfil `dev` contra un PostgreSQL vacío y desechable: 8 migraciones aplicadas, validación de Hibernate correcta, datos de demo cargados (6 usuarios, 33 productos). Login de encargado y personal y reglas comprobados con `curl`.

## 4. Para tu BD local

Al arrancar el backend en `dev` contra la BD de `docker compose`, Flyway la adopta como V1 y aplica V2 a V8: **convierte `products.category` en FK y borra la columna de texto**. Es lo esperado, pero no es reversible sin backup. Si tiene datos que te importen: `docker exec bar-and-go-postgres pg_dump -U <usuario> <bd> > backup.sql` antes de arrancar.

Si la BD es anterior a los cambios de esquema documentados en `docs/CAMBIOS.md` (enums en español), V2 fallará al aplicar el CHECK de roles; hay que recrearla (`docker compose down -v`).

## 5. Abierto para fases siguientes

- La versión (`@Version`) no viaja en los DTOs: protege frente a dos transacciones simultáneas, pero no frente a "abro el formulario, otro guarda, yo guardo". Cerrar eso exige enviar la versión al cliente y devolverla en el PUT. Anotado en el backlog para la Fase 6 (formularios de gestión).
- `order_item_link_issue` no tiene endpoint. Se revisa por SQL; si hiciera falta, un listado para ADMIN en la Fase 6.
- Borrar una `ItemCategory` en uso quedará bloqueado por la FK de la Fase 2 (409 vía el manejador de integridad); no hay comprobación previa en el servicio.
- No hay pantallas para proveedores, unidades ni categorías: entran en la Fase 6.
