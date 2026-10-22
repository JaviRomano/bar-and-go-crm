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

## Detectado en la Fase 4

- ~~**Registro persistente de eventos**~~: hecho en la Fase 5.
- **Alérgenos en la carta pública** se calculan en cada petición (carga artículos y escandallos). Suficiente con decenas de productos; con más, guardarlos al recalcular el escandallo.
- **Escandallos demo simplificados**: faltan artículos como el pan, así que los food cost demo salen bajos.

## Detectado en la Fase 5

- **Un solo almacén de ventas por local**: la bebida de barra y la comida de cocina salen del mismo almacén. Para descontar cada artículo de su almacén haría falta un almacén de salida por artículo o por familia.
- **Alerta de stock negativo** (D11): las ventas pueden dejar stock negativo y hoy solo se ve en la consulta de stock. Encaja en el panel de la Fase 6.
- **Eventos fallidos sin pantalla**: si un descuento de stock falla (por ejemplo, sin almacén de ventas), el evento queda pendiente y se reintenta al reiniciar. No hay vista ni reintento manual desde el panel.
- **Consumo entre recuentos**: solo cuentan los artículos contados en los dos recuentos, y el periodo va de cierre a cierre. Un recuento hecho con el almacén en marcha (ver Fase 3) desplaza los movimientos entre el conteo y el cierre.

## Detectado en la Fase 6

- **Tema oscuro**: `tokens.css` define `[data-theme="dark"]` y remite a un `ThemeProvider` descrito en un DESIGN.md (§BLOQUE 1) que no está en el repo. Sin él, la aplicación usa siempre el tema claro.
- **Pantallas anteriores al sistema de diseño** (carta, checkout, reservas, panel de pedidos): heredan los tokens a través de alias en `index.css`, pero mantienen estilos en línea y algún color fijo. Migrarlas a los componentes de Gestión.
- **Recepción contra pedido para el personal**: la matriz de permisos solo deja ver pedidos a proveedor al encargado, así que el personal registra el albarán línea a línea.
- **Coste de la ingeniería de menú**: usa el coste actual del escandallo (reposición), no el del momento de cada venta.
- **Food cost por almacén**: el filtro de almacén afecta al coste, no a las ventas, que no tienen almacén asociado.
- **Versión en los DTOs** sigue pendiente (Fase 1): las pantallas de Gestión no envían la versión que leyeron, así que dos personas editando la misma ficha pueden pisarse.
- **Tests de frontend**: siguen sin existir; las pantallas de Gestión se han comprobado contra la API, no con tests automatizados.
