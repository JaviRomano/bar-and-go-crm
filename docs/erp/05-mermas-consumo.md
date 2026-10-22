# Fase 5 — Mermas, descuento por venta y consumo

Rama: `feature/erp-fase-5` (desde `feature/erp-fase-4`). Alcance: descuento automático de stock al entregar un pedido, anulación si se cancela después, registro de mermas valoradas al PMP y consumo teórico frente a real entre dos recuentos (D15). Además, el registro persistente de eventos que quedó pendiente en la Fase 4.

## 1. Módulo `costcontrol`

Depende de `shared`, `identity`, `masterdata`, `purchasing`, `catalog`, `ordering`, `recipes` e `inventory`, y nadie depende de él. Pedidos no sabe que existe: publica eventos y control de costes los escucha.

| Migración | Contenido |
|---|---|
| `V19__event_publication` | Tabla del registro de eventos de Spring Modulith (DDL oficial para PostgreSQL), versionada con Flyway en lugar de dejar que la cree la librería |
| `V20__sales_stock_tracking` | `orders.delivered_at` (los ya entregados toman la fecha del pedido); `warehouse.default_for_sales` con un único almacén de ventas por local; índice único que impide descontar dos veces la misma línea de un pedido |
| `V21__cost_control` | Mermas (`waste`) y registro del descuento de cada pedido (`order_stock_posting`) |

## 2. Pedidos

- **Máquina de estados:** `CREATED → IN_PREPARATION → READY → DELIVERED`, y `CANCELLED` desde cualquiera de ellos. Saltar pasos o volver atrás da 409. Pedir el mismo estado no hace nada.
- **Entrega:** guarda `delivered_at` y publica `OrderDelivered` con el producto y la cantidad de cada línea.
- **Cancelar un pedido entregado:** solo encargado o administración (403 para el personal). Publica `OrderDeliveryReverted`.
- **Borrado (D8):** un pedido entregado alguna vez no se borra (409), se cancela.

## 3. Descuento de stock por venta

`OrderStockListener` escucha los dos eventos con `@ApplicationModuleListener`: de forma asíncrona, después del commit del pedido y en su propia transacción.

1. Si el pedido ya tiene fila en `order_stock_posting`, no hace nada (idempotencia).
2. Toma el almacén de ventas del local. Si no hay ninguno marcado, falla a propósito: el evento queda pendiente.
3. Descompone cada línea con el escandallo del producto (`RecipeService.consumptionForProduct`, con la misma explosión de sub-escandallos que la producción, D13) y suma por artículo.
4. Registra un movimiento `SALE` por artículo, con origen `ORDER` y el id del pedido, valorado al PMP. Puede dejar stock negativo (D11).
5. Guarda el coste total y cuántas líneas no tenían escandallo. Esas líneas no mueven stock, pero quedan contadas.

**Cancelación:** `StockPostingService.reverse` crea un movimiento opuesto por cada uno del pedido, al mismo coste unitario y enlazado con `reverses_movement_id`. Si la cancelación se procesa antes que la entrega (los eventos son asíncronos), se guarda la fila ya anulada y la entrega posterior no descuenta.

**Por qué el registro persistente de eventos:** sin él, un descuento de stock se perdería si la aplicación cae entre el commit del pedido y el del descuento. Con `spring-modulith-starter-jdbc`, el evento se guarda en la misma transacción que el cambio de estado y se borra al completarse (`completion-mode=delete`). Los pendientes se vuelven a publicar al arrancar (`republish-outstanding-events-on-restart=true`). Eso implica entregas duplicadas posibles, por eso la idempotencia tiene dos redes: la fila por pedido y el índice único de `V20` en los movimientos.

## 4. Mermas

`POST /api/waste`, abierto al personal: la merma la registra quien la ve.

- **De un artículo:** cantidad en cualquier unidad convertible (500 g de un artículo que se gestiona en kg).
- **De un plato:** en raciones; sale lo que dice su escandallo. Si el producto no tiene escandallo activo, 400.
- **Motivos:** `EXPIRED`, `BREAKAGE`, `PREPARATION`, `CUSTOMER_RETURN`, `COMPLIMENTARY`, `STAFF_MEAL`, `ORDER_ERROR`, `OTHER`.
- Genera movimientos `WASTE` al PMP y guarda el coste. No puede dejar stock negativo (D11): una merma de lo que no hay es un error de registro.
- Una merma no se edita ni se borra; se corrige con un ajuste.

## 5. Consumo teórico frente a real (D15)

`GET /api/cost-control/consumption?fromCountId=&toCountId=` con dos recuentos cerrados del mismo almacén. El periodo va del cierre del primero al cierre del segundo. Para cada artículo contado en los dos:

| Concepto | Cálculo |
|---|---|
| Real | contado inicial + compras + producción + traspasos de entrada − traspasos de salida − contado final |
| Teórico | ventas y consumos de producción, netos de anulaciones |
| Desviación | real − teórico, también en % sobre el teórico |
| Mermas registradas | salidas `WASTE` |
| Sin explicar | desviación − mermas: robo, raciones mal servidas, escandallo desajustado o mermas sin registrar |

Los valores se calculan al PMP actual del local. Las líneas se ordenan por el valor sin explicar, de mayor a menor. Los ajustes no cuentan, porque los recuentos ya miden el stock real. Si sumaran, el mismo hueco contaría dos veces.

El cálculo está aislado en `ConsumptionCalculator`, sin dependencias de Spring.

## 6. Permisos

| Ruta | STAFF | MANAGER |
|---|---|---|
| `POST /api/waste` | Sí | Sí |
| `GET /api/waste` | No | Sí |
| `GET /api/cost-control/**` | No | Sí |
| `PATCH /api/orders/{id}/status` a `CANCELLED` desde `DELIVERED` | No (403) | Sí |

## 7. Cambios transversales

- `StockPostingService` admite `SALE` y `WASTE` y añade `reverse`. `StockLedger.Entry` lleva `reversesMovementId`.
- `WarehouseDTO` incluye `defaultForSales`. Al marcar un almacén, se desmarca el anterior del mismo local.
- **Tests de integración:** la meta-anotación `@PostgresIntegrationTest` agrupa Testcontainers, Spring Boot y `@DirtiesContext`. El orden de esas anotaciones importa. Al cerrar, el registro de eventos de Modulith consulta los pendientes. Si el contexto seguía en caché después de parar su contenedor, el cierre de la JVM esperaba 30 s por una conexión y Surefire acababa matando el proceso. Ahora cada contexto se cierra antes de parar su contenedor.

## 8. Datos demo

Con `dev`, la cámara es el almacén de ventas. Los pedidos demo que se entreguen desde el panel descuentan stock según los escandallos de la Fase 4.

## 9. Verificación

- `./mvnw test` en local con JDK 25: 227 tests, 0 fallos, 0 omitidos, sin esperas al cerrar la JVM.
  - Cálculo: `ConsumptionCalculatorTest` cubre consumo real con traspasos, mermas que explican parte de la desviación, ventas anuladas, producción (teórico para los ingredientes, entrada para el resultado), ajustes ignorados y porcentaje sin teórico.
  - Pedidos: `OrderServiceImplTest` cubre el evento de entrega con sus líneas, pasos saltados, mismo estado, cancelación de un entregado (403 al personal, evento al encargado) y borrado de un entregado.
  - Integración: `CostControlFlowTests` recorre recepción, recuento inicial, pedido entregado con un producto sin escandallo, reenvío del evento sin doble descuento, pedido entregado y cancelado, mermas por artículo en gramos y por plato, merma sin stock rechazada, recuento final e informe con 2 botellines sin explicar.
  - `SecurityRulesTest`: mermas y control de costes por rol.
- Arranque real en `dev` con PostgreSQL desechable: 21 migraciones; pedido entregado → movimiento `SALE`, merma, 403 al personal al cancelar, cancelación del encargado → movimiento compensatorio. Al terminar, el registro de eventos queda vacío.
- `npm run build` y `npm run lint`: el frontend no cambia en esta fase.
