# Fase 3 — Inventario

Rama: `feature/erp-fase-3` (desde `feature/erp-fase-2`). Alcance: almacenes, ledger de movimientos, valoración PMP, lotes y caducidades, recepción de albaranes (traída de la Fase 2 según D10), ajustes, traspasos, recuentos y sugerencia de reposición. Además, datos demo de ERP y su valoración.

## 1. Módulo `inventory`

Depende de `shared`, `identity`, `masterdata` y `purchasing`. Compras no depende de inventario: cuando compras necesita saber algo del stock (si un artículo tiene movimientos), define la pregunta (`StockItemUsageCheck`) e inventario la responde, el mismo patrón que `ProductSalesCheck` en la Fase 1.5.

| Migración | Contenido |
|---|---|
| `V12__purchase_receipt_tracking` | Compras: cantidad recibida por línea de pedido e histórico de precios de compra |
| `V13__sites_and_warehouses` | Local (uno sembrado, multi-local preparado) y almacenes |
| `V14__stock_ledger` | Lotes, movimientos (con trigger de inmutabilidad), saldos y valoración |
| `V15__goods_receipts` | Albaranes y sus líneas |
| `V16__stock_counts` | Recuentos y sus líneas; un solo recuento abierto por almacén (índice único parcial) |

## 2. Ledger de movimientos

- **Inmutable** en tres niveles: la entidad no tiene setters, Hibernate la trata como `@Immutable` y un trigger de PostgreSQL rechaza cualquier `UPDATE` o `DELETE` (comprobado en el test de integración, también desde SQL directo). Un error se corrige con un movimiento nuevo.
- **`StockLedger` es la única vía para cambiar stock.** Cada línea genera un movimiento y, en la misma transacción, actualiza el saldo del almacén, la valoración PMP del local y los lotes.
- **Concurrencia:** antes de aplicar nada se bloquean (`SELECT ... FOR UPDATE`) todas las filas de saldo y valoración que va a tocar la operación, siempre en el mismo orden, para que dos operaciones simultáneas no se bloqueen mutuamente. La fila se crea si no existe con `INSERT ... ON CONFLICT DO NOTHING`.
- **Stock negativo (D11):** permitido en ventas y consumos de producción (fases 4 y 5), bloqueado en ajustes, mermas y traspasos.
- Cada operación agrupa sus movimientos con un `operation_id` (un albarán, un traspaso...) y guarda el documento de origen (`source_type`, `source_id`, `source_line_id`).

Tipos de movimiento (correspondencia con el encargo): `PURCHASE_RECEIPT` (entrada de compra), `SALE` (venta), `WASTE` (merma), `ADJUSTMENT` (ajuste de inventario), `TRANSFER` (traspaso), `PRODUCTION_CONSUMPTION` y `PRODUCTION_OUTPUT` (producción).

## 3. Valoración PMP (D1)

PMP perpetuo por artículo y local (`MovingAverage`):

- Una entrada recalcula el precio medio: (q_prev x pmp_prev + q_in x c_in) / (q_prev + q_in).
- Con stock previo cero o negativo, el PMP pasa a ser el coste de la entrada; promediar con cantidades negativas daría precios absurdos.
- Las salidas se valoran al PMP vigente y no lo cambian.
- Un traspaso dentro del local sale y vuelve a entrar al PMP vigente: no lo cambia.
- Seis decimales en el coste unitario; los importes se redondean a céntimos al presentarlos.

## 4. Lotes y caducidad (D12)

- Los artículos perecederos exigen lote y fecha de caducidad al recibirlos.
- Las salidas consumen lotes por FEFO (`LotAllocator`): primero el que caduca antes, sin fecha al final y, a igual fecha, el que entró antes. Es informativo: la valoración sigue siendo PMP. Lo que no está en ningún lote queda sin asignar.
- En un traspaso, los lotes viajan con la mercancía: se recrean en destino con el mismo código y caducidad.
- `GET /api/inventory/lots/expiring?days=N`: lotes con existencias que caducan en N días, incluidos los ya caducados.

## 5. Recepción de albaranes

- Se registra contra un pedido enviado o sin pedido. Con pedido, cada línea se cruza con la línea del pedido de su artículo (una por artículo, regla de la Fase 2), y de ella salen unidad y precio por defecto; sin pedido, de la tarifa del proveedor.
- Cantidad y coste se pasan a unidad de inventario: 1 caja a 7,20 € son 24 ud a 0,30 €.
- Al registrarlo, en la misma transacción: entradas de stock, lotes, cantidad recibida en el pedido (que pasa a `PARTIALLY_RECEIVED` o `RECEIVED`), histórico de precios y tarifa del proveedor (el último precio pagado pasa a ser la tarifa).
- El personal recibe mercancía, pero un precio distinto del del pedido o la tarifa solo lo registra un encargado (403).
- Un albarán registrado no se modifica ni se borra; un error se corrige con un ajuste. Un pedido al que no llegará el resto se cierra con `POST /api/purchase-orders/{id}/close`.

## 6. Ajustes, traspasos y recuentos

- **Ajuste manual** (MANAGER): cantidad con signo y motivo obligatorio; un ajuste positivo puede llevar coste, si no se valora al PMP.
- **Traspaso** (STAFF): entre almacenes del mismo local; sin stock suficiente en origen, 400.
- **Recuento:** el encargado lo abre (hoja con los artículos que han tenido stock en ese almacén), el personal registra cantidades (se pueden añadir artículos) y el encargado lo cierra. Al cerrar, el teórico se toma con el saldo bloqueado y cada diferencia genera un ajuste. Quien cuenta no ve el teórico ni los costes, para no contar lo que espera encontrar. Las líneas sin contar no se ajustan.
- Supuesto de uso: se cuenta con el almacén parado. Si se vende mientras se cuenta, esa venta acaba dentro de la diferencia.

## 7. Reposición

- Disponible = stock del local + pedido y pendiente de recibir (pedidos enviados o recibidos en parte). Si no llega al mínimo, se sugiere hasta el máximo (o hasta el mínimo si no hay máximo), redondeado a unidades de compra enteras (`ReplenishmentCalculator`).
- Proveedor: el habitual si tiene tarifa; si no, el de tarifa más barata por unidad de inventario; solo proveedores activos. Sin tarifa, el artículo va a un grupo "sin proveedor" para pedirlo a mano.
- `POST /api/inventory/replenishment/orders` crea un pedido en borrador por proveedor, para revisarlo antes de enviarlo.

## 8. Permisos

| Ruta | STAFF | MANAGER |
|---|---|---|
| `GET /api/warehouses` | Sí | Sí, y alta/edición |
| `GET /api/inventory/stock`, `/lots/expiring` | Sí (cantidades, sin costes) | Sí |
| `GET /api/inventory/valuation`, `/movements` | No | Sí |
| `POST /api/inventory/transfers` | Sí | Sí |
| `POST /api/inventory/adjustments` | No | Sí |
| `GET/POST /api/inventory/replenishment` | No | Sí |
| `POST /api/goods-receipts` | Sí (sin cambiar precios) | Sí |
| `GET /api/goods-receipts` | No | Sí |
| `POST /api/stock-counts`, `/{id}/close` | No | Sí |
| `GET /api/stock-counts`, `PUT /{id}/lines` | Sí (sin teórico ni costes) | Sí |

## 9. Datos de prueba: cuándo merece la pena

| Tipo | Decisión | Motivo |
|---|---|---|
| Datos de tests automáticos | Cada test crea lo suyo a través de la API, con un helper común (`ErpTestApi`) | Datos sembrados compartidos acoplan los tests entre sí y los vuelven frágiles. El helper evita repetir la preparación (categoría, proveedor, artículo, almacén, pedido) |
| Datos demo de ERP en `dev` | Ahora, al cerrar la Fase 3 | Es la primera fase con estado observable (stock, PMP, lotes, reposición). Antes solo habría maestros sueltos. Se ampliarán en las fases 4 y 5 con escandallos de la carta demo y ventas |
| Volumen e histórico (meses de movimientos) | Fase 6 | Solo los informes (consumo teórico frente a real, rotación, evolución de precios) lo necesitan |

Los datos demo se cargan en `dev` por módulo y en orden (`@Order`): usuarios (0), carta (5), datos maestros (10), compras (20), inventario (30). Cada inicializador solo actúa si su parte está vacía y lo crea todo a través de los servicios, con sus validaciones:

- 6 categorías de compra, 3 proveedores y 13 artículos con tarifa, pensados para la carta demo.
- 4 almacenes, 3 pedidos recibidos (uno en parte), traspasos a barra, lotes que caducan en uno y dos días, y un artículo bajo mínimo para que la reposición sugiera algo.

Hasta la Fase 6 no hay pantallas de ERP: estos datos se consultan por la API.

## 10. Verificación

- `./mvnw test` en local con JDK 25: 193 tests, 0 fallos, 0 omitidos, sin avisos de la JVM.
  - Cálculo: `InventoryCalculationsTest` (PMP, FEFO y reposición) y los nuevos casos de `PurchaseOrderLifecycleTest` (recepción parcial, total, de más y cierre con faltas).
  - Integración: `InventoryFlowTests` recorre recepción parcial, perecedero sin lote, precio cambiado por el personal (403) y por el encargado, PMP 0,35, tarifa e histórico actualizados, traspasos con lotes, stock insuficiente, ajuste, recuento con su ajuste, reposición con pedido en borrador, bloqueo de la unidad de inventario e inmutabilidad del ledger desde SQL.
  - `ModularityTests`: `inventory` depende de `shared`, `identity`, `masterdata` y `purchasing`; nada depende de `inventory`.
- Arranque real en `dev` contra PostgreSQL vacío: 16 migraciones, los cinco inicializadores de demo en orden y consultas de stock, valoración, caducidades, reposición y pedidos con encargado y personal.
- `npm run build` y `npm run lint`: sin errores (el frontend no cambia en esta fase).

## 11. Para fases siguientes

- **Fase 4:** la producción de elaboraciones usará `PRODUCTION_CONSUMPTION` y `PRODUCTION_OUTPUT` a través de `StockLedger`, que habrá que exponer en la API del módulo.
- **Fase 5:** el descuento por venta y su compensación usarán `SALE` y `reverses_movement_id`, con el índice único de idempotencia de la Fase 0 (3.2) y, según la evaluación de Modulith, un evento de pedidos con registro de publicaciones.
- **Fase 5:** el consumo real necesita dos recuentos cerrados que delimiten el periodo (D15).
