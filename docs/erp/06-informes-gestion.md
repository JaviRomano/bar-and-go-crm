# Fase 6 — Informes y pantallas de gestión

Rama: `feature/erp-fase-6` (desde `feature/erp-fase-5`). Alcance: endpoints de informes con filtros de fecha y almacén, y la sección «Gestión» del panel con las pantallas de todo el ERP, sobre el sistema de diseño de Bar&Go.

## 1. Módulo `reporting`

Solo lee: no tiene tablas ni escribe nada. Depende de los módulos de los que saca datos y nadie depende de él. Para no recorrer movimiento a movimiento, se añaden dos consultas a otros módulos:

- **Inventario:** `getMovementTotals(warehouseId, from, to)` agrega el ledger por artículo, almacén y tipo en la base de datos (`MovementTotalsQuery`, Criteria API). Sin `from`, la suma es la posición a la fecha `to`: cantidad y valor contable, porque cada movimiento ya lleva su valor a PMP.
- **Pedidos:** `getDeliveredSales(from, to)` devuelve las líneas de los pedidos entregados en el periodo que siguen entregados.
- **Mermas:** `getWaste` acepta filtro por almacén.

Los periodos son días en la zona del local (`app.reports.zone`, por defecto `Europe/Madrid`), con los dos extremos incluidos. Sin fechas, los últimos 30 días.

## 2. Informes

| Informe | Cálculo |
|---|---|
| Valor de stock | Suma de los movimientos hasta el final del día elegido, por artículo, categoría y almacén. Coste medio = valor / cantidad |
| Food cost | Ventas sin IVA (con el tipo del producto). Teórico = salidas `SALE`, lo que dicen los escandallos. Real = teórico + mermas + ajustes y diferencias de recuento + producción neta, que equivale a stock inicial + compras − stock final. Serie por día hasta 31 días y por semana a partir de ahí |
| Mermas | Por motivo (con su peso), por lo registrado (artículo o plato) y por artículo, con los platos ya descompuestos en ingredientes |
| Rotación | Consumo (ventas, mermas y consumo de producción) / stock medio, con stock medio = (inicial + final) / 2. Cobertura = días del periodo / rotación. Primero lo que no se mueve |
| Precios de compra | Precio pagado en cada compra, pasado a unidad de inventario con las conversiones del artículo (una caja de 24 a 12 € es 0,50 €/ud). Primero, último, mínimo, máximo y variación |
| Ingeniería de menú | Kasavana y Smith. Popular si su cuota de ventas llega al 70 % de la cuota media; rentable si su margen unitario llega al margen medio ponderado por ventas. Estrella, vaca (popular y poco margen), puzzle (margen y pocas ventas), perro |

Decisiones:

- **El food cost real sale del ledger, no de dos recuentos.** Es la fórmula clásica del sector (inicial + compras − final) y admite cualquier periodo. Es fiable cuando hay recuentos cerrados cerca de los extremos: sin ellos, lo que falta y nadie ha registrado no aparece. El informe de consumo de la Fase 5 (`/api/cost-control/consumption`) sigue siendo el que exige recuentos.
- **Las ventas de productos sin escandallo** cuentan como ingreso pero no como coste teórico, así que bajan el food cost teórico. El informe devuelve ese importe aparte y la pantalla lo avisa.
- **La ingeniería de menú usa el coste actual del escandallo** (reposición) y el precio medio cobrado en el periodo. Sin ventas, el precio de la carta. Un producto archivado sale de la matriz.

## 3. Frontend: sección «Gestión»

Rutas `/admin/gestion/*`, con `React.lazy`: el código de cada pantalla se descarga al entrar y no pesa en la carta ni en el panel de sala.

| Pantalla | STAFF | MANAGER |
|---|---|---|
| Proveedores | — | Alta y edición |
| Artículos | Consulta | Alta y edición, conversiones, alérgenos, precios por proveedor e historial de compras |
| Compras | — | Pedidos (borrador, envío, cancelación, cierre con faltas) y sugerencia de reposición |
| Recepción | Registrar albarán sin tocar precios | Además, partir de un pedido enviado y ver los albaranes |
| Inventario | Stock, traspasos, caducidades | Además, movimientos, valoración, ajustes y almacenes (incluido el de ventas) |
| Recuentos | Contar | Abrir, cerrar y ver diferencias; consumo teórico frente a real entre dos recuentos |
| Escandallos | Consulta sin costes y producción | Alta y edición, costes, food cost, histórico |
| Mermas | Registrar | Además, el listado valorado con filtros |
| Informes | — | Los seis informes |

- **`ConfirmModal` en toda acción que mueve stock:** recepción, traspaso, ajuste, cierre de recuento, producción, merma, y en Pedidos, entregar (descuenta escandallos) y cancelar un pedido entregado (lo devuelve; solo encargado).
- **Errores de validación por campo:** el backend devuelve `validationErrors` con claves como `supplierId` o `lines[0].quantity`. El hook `useForm` los guarda y cada campo muestra el suyo. Las líneas de pedidos, albaranes y escandallos usan la clave con índice.
- **Piezas compartidas** en `src/gestion/`: cliente de la API (`api.js`), carga y formularios (`support.js`), catálogos y confirmación (`lookups.js`), componentes (`ui.jsx`), gráficas (`charts.jsx`) y etiquetas de los enums (`labels.js`).

## 4. Sistema de diseño

Los ficheros de diseño llegaron con los nombres cruzados: `frontend/DESIGN.md` contiene el `tokens.ts` y `frontend/bargo-favicon.svg` contiene el `tokens.css`. No se han modificado ni se suben al repositorio. Se ha usado su contenido así:

- `src/styles/tokens.css`: copia del `tokens.css`, la fuente de verdad (colores con tema claro y oscuro, Sora e Inter, espaciado base 4, radios, tamaños táctiles, capas). Sin colores sueltos fuera de él.
- `src/design/tokens.js`: versión JS del `tokens.ts`. **Los estados de pedido se alinean con el enum del backend** (`CREATED`, `IN_PREPARATION`...), que era la decisión que el fichero dejaba pendiente.
- `index.css` se reescribe sobre los tokens y mantiene las clases y variables que usan las pantallas anteriores (`--primary-color` pasa a ser alias de `--accent-text`, etc.), así que toda la aplicación adopta la paleta sin reescribir esas pantallas.
- Marca: el símbolo del logotipo (`Bar&Go — Logo.html`) como componente `BrandMark`, el favicon en `public/favicon.svg` y el nombre en Sora con el «&» en ámbar.

Desviaciones, pendientes de validar con diseño:

- **Comparativa en gráficas:** el `tokens.ts` fija `surface-2` para la serie comparada, pero sobre `--surface` no se distingue (1,1:1). Se usa una línea de 2 px en `--muted`.
- **Ámbar en gráficas:** el validador de paletas da 1,95:1 contra la superficie. Por eso cada gráfica lleva el valor escrito y una tabla con los mismos datos.
- **Tokens nuevos `--on-mark*`:** texto y estados sobre el fondo de marca, oscuro en los dos temas. Valores tomados del logotipo, en un bloque marcado al final de `tokens.css`.
- **Tema oscuro:** los tokens lo soportan, pero el `ThemeProvider` al que remite el fichero no está descrito en el repo. Va al backlog.

## 5. Datos demo

Con `dev`, después de los escandallos (`DemoCostControlInitializer`, `@Order(50)`): cuatro pedidos de los clientes demo entregados por el personal y dos mermas (tomate caducado y unas bravas por error de comanda). Con eso, todos los informes tienen datos al arrancar. En la matriz salen estrellas, vacas y puzzles.

## 6. Verificación

- `./mvnw test` en local con JDK 25: 239 tests, 0 fallos, 0 omitidos.
  - Cálculo: `ReportCalculationsTest` cubre umbrales de la matriz (70 % de la cuota media, margen ponderado), los cuatro cuadrantes, el caso límite, la matriz sin ventas, el IVA, los porcentajes sin base y la rotación con y sin consumo.
  - Integración: `ReportsFlowTests` comprueba importes calculados a mano. Dos compras de cerveza a 1,00 y 1,50 (PMP 1,25), ventas por 12,50 sin IVA, food cost teórico del 34 % y real del 44 % con una merma, valor de stock de 74,50, rotación, una subida de precio del 50 % y la clasificación vaca/puzzle.
  - `SecurityRulesTest`: informes solo para el encargado.
- Arranque real en `dev` con PostgreSQL desechable: los seis cargadores de demo y el de ventas, y los 23 endpoints que usa el frontend con sus mismos parámetros (200 en todos). Errores de validación con las claves que esperan los formularios.
- `npm run lint` y `npm run build`: sin errores. Una pantalla de Gestión pesa entre 3,4 y 14 kB; el bundle inicial apenas cambia.
- **No comprobado:** la revisión visual en navegador. Las pantallas se han validado contra la API real, no con capturas.
