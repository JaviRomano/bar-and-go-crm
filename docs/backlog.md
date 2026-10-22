# Backlog

Puntos detectados fuera del alcance de la fase en curso. Se priorizan cuando se cierre la iniciativa ERP o antes si se confirma.

## Fuera de alcance de la iniciativa ERP (D5)

- Caja y cierre diario.
- Personal y turnos.
- APPCC y trazabilidad sanitaria.
- Facturación electrónica.
- Integración con TPV externo.

## Detectado en la Fase 0

- **Importación de ventas agregadas** (CSV por producto y día). Sin TPV, las únicas ventas registradas son los pedidos web; el consumo teórico queda muy por debajo del real en un local con venta en barra.
- **Token en `localStorage`**. Valorar cookie `HttpOnly` + protección CSRF.
- **Revocación de rol**. El rol viaja en el JWT; un cambio de rol no se aplica hasta que caduca el token.
- **Reservas asociadas por teléfono**. `Reservation` no tiene FK a `User`; si el cliente cambia de teléfono pierde sus reservas.
- **Fechas sin zona horaria** en entidades existentes (`LocalDateTime`). Las tablas nuevas usan `timestamptz`.
- **Sin tests de frontend.**
- **Devolución a proveedor** como tipo de movimiento de stock (no está en la lista de la Fase 3).

## Detectado en la Fase 1

- **Versión en los DTOs** (Fase 6). `@Version` protege transacciones simultáneas, pero el cliente no envía la versión que leyó, así que dos personas editando el mismo formulario siguen pudiendo pisarse.
- **Revisión de `order_item_link_issue`**. Líneas antiguas sin producto enlazado; hoy solo consultables por SQL.

## Detectado en la Fase 3

- **Devolución a proveedor** sigue pendiente (ver Fase 0): hoy una devolución se registra como ajuste negativo, sin documento propio ni efecto en el pedido.
- **Recuento con actividad**: el cierre asume que se ha contado con el almacén parado. Para contar en marcha haría falta fijar un corte y descontar los movimientos posteriores.
- **Listados sin paginar** (artículos, pedidos, albaranes, recuentos, movimientos). Suficiente para un local; si crecen, paginación en servidor.
