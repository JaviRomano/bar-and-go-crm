# Fase 2 — Mercancía y compras

Rama: `feature/erp-fase-2` (desde `feature/erp-fase-1-5`). Alcance según D10: artículos de compra, tipos de IVA, alérgenos, precios por proveedor y pedidos a proveedor. La recepción de albaranes y la sugerencia de reposición pasan a la Fase 3, porque necesitan el inventario.

## 1. Qué se ha hecho

### Datos maestros (`masterdata`)

- **Tipos de IVA** (`VatRate`, V9): tabla configurable con General 21 %, Reducido 10 %, Superreducido 4 % y Exento 0 %. ADMIN los modifica y MANAGER los consulta. Sin borrado: se desactivan. Cambiar un porcentaje no altera pedidos ya hechos, porque cada línea guarda el suyo.
- **Alérgenos** (`Allergen`): los 14 del Reglamento (UE) 1169/2011, anexo II. Es un enum en la API de `masterdata`, porque los usarán también la carta y los escandallos (Fase 4).
- Consultas nuevas en la API para el módulo de compras: `UnitOfMeasureService.getUnitByCode` e `ItemCategoryService.getCategory`.
- Borrar una categoría de artículo en uso devuelve 409. La FK de `stock_item` lo impide; el servicio fuerza el flush para que el error salga como conflicto y no al hacer commit.

### Compras (`purchasing`, módulo nuevo)

| Elemento | Detalle |
|---|---|
| `StockItem` (V10) | Código único (normalizado a mayúsculas), nombre, categoría, unidad de compra, de inventario y de uso, conversiones propias, proveedor habitual, IVA, stock mínimo y máximo (en unidad de inventario), alérgenos, perecedero, activo |
| Conversiones propias | `stock_item_unit_conversion`: 1 caja = 24 ud, 1 ud = 0,75 l... Prevalecen sobre las genéricas para ese artículo |
| Regla de unidades | Un artículo solo se guarda si su unidad de compra y su unidad de uso se pueden convertir a su unidad de inventario. Sin eso, la recepción (Fase 3) y los escandallos (Fase 4) no podrían pasar cantidades a stock |
| `StockItemSupplier` (V10) | Precio sin IVA del artículo en cada proveedor, en la unidad en la que vende ese proveedor, con su referencia de catálogo. La unidad tiene que ser convertible a la de inventario |
| `PurchaseOrder` (V11) | Número `PC-<año>-<secuencia>` de una secuencia propia; proveedor; fechas de pedido y de entrega prevista; notas; usuario que lo crea; líneas |
| Líneas | Artículo, unidad, cantidad, precio e IVA. Unidad y precio, si no se indican, salen de la tarifa del proveedor; si se pide en otra unidad, el precio se convierte (tarifa 12 € la caja de 24 → 0,50 € la unidad). El IVA se copia del artículo |
| Importes | `PurchaseOrderTotals`: importe de línea redondeado a céntimos; base e IVA agrupados por tipo, con el IVA calculado sobre la base de cada tipo (como en factura) y redondeo HALF_UP |
| Fecha prevista | Si no se indica, fecha del pedido más el plazo de entrega habitual del proveedor |

Estados del pedido:

```
DRAFT ──> SENT ──> PARTIALLY_RECEIVED ──> RECEIVED
  │         │  └──────────────────────────────^
  └─────────┴──> CANCELLED
```

- Solo un borrador se modifica o se borra. Un pedido enviado se cancela, no se borra.
- `PARTIALLY_RECEIVED` y `RECEIVED` los fijará la recepción de albaranes (Fase 3). Con algo recibido ya no se cancela: se da por recibido aunque falte mercancía.

### Seguridad

| Ruta | Lectura | Escritura |
|---|---|---|
| `/api/vat-rates` | MANAGER | ADMIN |
| `/api/stock-items` | STAFF | MANAGER |
| `/api/stock-items/{id}/suppliers` | MANAGER (el personal no ve precios de compra) | MANAGER |
| `/api/purchase-orders` | MANAGER | MANAGER |

## 2. Decisiones tomadas en la fase

| Tema | Decisión | Motivo |
|---|---|---|
| Conversiones por artículo (D17) | Tabla propia de `purchasing` (`stock_item_unit_conversion`), no una columna en `unit_conversion` | `unit_conversion` es de `masterdata`; una FK suya hacia artículos invertiría la dependencia entre módulos |
| Relación con datos maestros | Por id con FK en base de datos, sin relaciones JPA entre módulos | Regla fijada en la Fase 1.5. La integridad la garantiza la BD y los módulos no comparten entidades |
| Alérgenos | Enum en `masterdata`, no tabla | Son una lista cerrada por reglamento; un cambio de ley es un cambio de código y de migración, no de configuración |
| Unidades en la API | Por código (`caja`, `ud`) | Es como las escribe una persona y como ya funcionaba `/api/units/convert` |
| Línea duplicada | Un artículo solo puede aparecer en una línea del pedido | Evita ambigüedad al recibir (Fase 3): cada línea recibida se cruza con una sola línea pedida |
| Filtros de listados | Se filtran en memoria | Volúmenes de un local (cientos de artículos, decenas de pedidos al mes). Si crecen, se pasan a consultas paginadas |

## 3. Corrección incluida

El cambio a Java 25 configuró el agente de Mockito sin comillas en `argLine`. En Windows, con un perfil de usuario con espacios (`C:\Users\francisco 3.2`), la JVM de tests no arrancaba. En Docker no salió porque la ruta era `/root/.m2`. Corregido entrecomillando la ruta.

## 4. Verificación

- `./mvnw test` en local con JDK 25: 162 tests, 0 fallos, 0 omitidos, sin avisos de la JVM.
  - Cálculo: `PurchaseOrderTotalsTest` (redondeos, agrupación por tipo, IVA sobre la base agrupada frente a línea a línea, conversión de precio entre unidades) y `PurchaseOrderLifecycleTest` (matriz completa de transiciones).
  - Servicios: `StockItemServiceImplTest` y `PurchaseOrderServiceImplTest`, con datos maestros simulados y el conversor de unidades real.
  - Seguridad: reglas de compras e IVA en `SecurityRulesTest`.
  - Integración: `PurchasingFlowTests` contra PostgreSQL con las migraciones V9 a V11, desde la categoría hasta el pedido cancelado, más el 409 al borrar una categoría en uso.
  - `ModularityTests`: `purchasing` depende solo de `shared`, `identity` y `masterdata`.
- `npm run build` y `npm run lint`: sin errores (el frontend no cambia en esta fase).

## 5. Para fases siguientes

- **Fase 3:** la recepción de albaranes moverá los pedidos a `PARTIALLY_RECEIVED` o `RECEIVED`, actualizará el último precio de compra y el histórico, y generará las entradas de stock. La sugerencia de reposición usará `minStock` y `maxStock`.
- **Fase 3:** impedir cambiar la unidad de inventario de un artículo que ya tenga movimientos; hoy se puede, porque aún no hay stock.
- **Fase 4:** los escandallos usarán la unidad de uso y los alérgenos de los artículos, y `Product` recibirá su tipo de IVA de venta.
