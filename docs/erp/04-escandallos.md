# Fase 4 — Escandallos

Rama: `feature/erp-fase-4` (desde `feature/erp-fase-3`). Alcance: escandallos de platos y elaboraciones, sub-escandallos con detección de ciclos, coste y food cost, PVP sugerido, histórico de coste con recálculo automático, alérgenos propagados a la carta, IVA de venta del producto y producción de elaboraciones.

## 1. Módulo `recipes`

Depende de `shared`, `identity`, `masterdata`, `purchasing`, `catalog` e `inventory`, y nadie depende de él. Lo que aporta a otros módulos lo hace sin crear dependencias hacia sí mismo:

- **Carta:** la carta define `ProductAllergenSource` y escandallos la implementa, así que la carta muestra los alérgenos de cada producto sin depender de escandallos.
- **Compras:** compras publica `StockItemCostChanged` cuando puede cambiar el coste de un artículo (tarifa, compra registrada, proveedor habitual, conversiones). Escandallos lo escucha con `@ApplicationModuleListener`: de forma asíncrona, después del commit de compras y en su propia transacción. Compras no sabe quién escucha y un fallo al recalcular no deshace la recepción.

| Migración | Contenido |
|---|---|
| `V17__product_vat_rate` | IVA de venta en `products` (D14); los existentes pasan al 10 % |
| `V18__recipes` | Escandallos, líneas, histórico de coste y lotes de producción, con restricciones de forma (un plato tiene producto y raciones; una elaboración, rendimiento y unidad; cada línea, artículo o sub-escandallo) |

## 2. Modelo

| Tipo | Rinde | Uso |
|---|---|---|
| `DISH` | Raciones | Escandallo de un producto de la carta (uno por producto). Bebidas de reventa: una línea con el artículo |
| `PREPARATION` | Cantidad en una unidad (2 kg) | Elaboración intermedia usada como sub-escandallo. Con artículo de stock asociado, se produce por adelantado y se guarda |

Cada línea lleva cantidad bruta, unidad y rendimiento (% que queda tras limpiar). La cantidad neta se calcula.

## 3. Cálculo (`RecipeCosting`, `MenuPricing`)

- **Coste de un ingrediente:** su coste de reposición por unidad de inventario. Lo da compras con `getReferenceCost`: la tarifa del proveedor habitual o, si no hay, la más barata entre proveedores activos. Tras cada recepción, la tarifa es el último precio pagado.
- **Sobre la cantidad bruta:** el coste se calcula sobre lo que se compra, y lo que se pierde al limpiar va incluido. La neta es lo que llega al plato.
- **Sub-escandallos:** cuestan su coste por unidad de rendimiento multiplicado por la cantidad usada, convertida a esa unidad.
- **Coste incompleto:** un artículo sin tarifa no suma. El escandallo queda marcado como incompleto con la lista de artículos que faltan, y no se calculan food cost ni PVP sugerido sobre un coste falso.
- **Food cost:** coste por ración sobre el PVP sin IVA. Margen bruto: PVP sin IVA menos coste.
- **PVP sugerido:** coste dividido por el food cost objetivo, más IVA. El objetivo por defecto es `app.recipes.target-food-cost-pct=30` y cada escandallo puede fijar el suyo.
- **Alérgenos:** unión de los de todos los ingredientes, sub-escandallos incluidos.

Decisión: el escandallo usa coste de reposición y no PMP. Responde a "cuánto me cuesta hacerlo hoy". El consumo real (Fase 5) se valora a PMP con los movimientos, y la comparación entre ambos es precisamente el informe de la Fase 6.

## 4. Ciclos

Una elaboración no puede usarse, directa ni indirectamente, como ingrediente de sí misma. Al guardar se recorre el grafo de sub-escandallos (`RecipeGraph`) y se rechaza con la ruta del ciclo en el mensaje ("Salsa -> Base -> Salsa"). El cálculo de coste y la explosión de ingredientes tienen además su propia guarda por si un ciclo llegara a la base de datos por otra vía.

## 5. Histórico y recálculo

- Se guarda una fila de histórico cada vez que cambia el coste: al crear, al editar, al cambiar un precio de compra (por el evento) o con `POST /api/recipes/recalculate`. Si el coste no ha cambiado, no se guarda.
- El recálculo por evento afecta a los escandallos que usan el artículo y a los que los usan como sub-escandallo.
- **Pendiente:** sin el registro persistente de eventos, un recálculo se perdería si la aplicación cae justo después de un cambio de precio. El recálculo manual lo recupera. El registro se introduce en la Fase 5, donde perder el evento sí sería grave (descuento de stock).

## 6. Producción

`POST /api/recipes/{id}/production` sobre una elaboración con artículo de stock:

1. Calcula los ingredientes necesarios para la cantidad pedida (`stockConsumption`). Un sub-escandallo inventariable se consume de su stock; uno no inventariable se descompone en sus ingredientes (D13).
2. Registra un `PRODUCTION_CONSUMPTION` por ingrediente, valorado al PMP. Puede dejar stock negativo (D11).
3. Da de alta la elaboración con `PRODUCTION_OUTPUT` al **coste real** de lo consumido, no al teórico del escandallo.
4. Guarda el lote de producción. Todos los movimientos comparten `operation_id`.

Inventario expone para ello `StockPostingService`, que la Fase 5 usará para ventas y mermas.

## 7. Carta

- `ProductDTO` incluye `vatRateId`, `vatRate` y `allergens`. Si no se indica IVA, se aplica el del 10 %. El formulario actual del frontend no lo envía y sigue funcionando.
- Los alérgenos aparecen también en la carta pública (`GET /api/products`), que es donde la normativa exige informarlos.

## 8. Permisos

| Ruta | STAFF | MANAGER |
|---|---|---|
| `GET /api/recipes`, `/{id}` | Sí, sin costes ni precios | Sí, con costes |
| `POST /api/recipes/{id}/production` | Sí | Sí |
| `POST`, `PUT /api/recipes` | No | Sí |
| `GET /api/recipes/{id}/cost-history`, `POST /recalculate` | No | Sí |

## 9. Cambios transversales

- `spring-modulith-events-api` para `@ApplicationModuleListener`. `@EnableAsync` está en `shared` y los hilos virtuales, activados (`spring.threads.virtual.enabled=true`): con Java 25 ya no tienen el problema de bloqueo de `synchronized` de versiones anteriores.
- Compras centraliza el coste de referencia y el conversor por artículo (`StockItemUnits`). La reposición de la Fase 3 y el catálogo de inventario los reutilizan en lugar de tener su propia copia.

## 10. Datos demo

Con `dev`, después del inventario:

- Una elaboración "Salsa de tomate casera" con artículo de stock y una producción de 1 kg en la cámara.
- Escandallos de Pizza Margarita, Hamburguesa Clásica, Patatas Bravas, Pollo a la Plancha y las bebidas de reventa.

Los escandallos demo están simplificados: por ejemplo, la hamburguesa no lleva pan porque el pan no está entre los artículos demo. Por eso los food cost salen bajos.

## 11. Verificación

- `./mvnw test` en local con JDK 25: 213 tests, 0 fallos, 0 omitidos, sin avisos de la JVM.
  - Cálculo: `RecipeCalculationsTest` cubre coste bruto y neto, raciones, sub-escandallos, propagación de alérgenos, coste incompleto, ciclos (directo, indirecto, diamante sin ciclo, ciclo ajeno), consumo escalado, sub-escandallo inventariable frente a no inventariable y precios.
  - Integración: `RecipesFlowTests` recorre coste con sub-escandallo y merma, food cost 25,10 % y PVP sugerido, alérgenos en la carta pública, costes ocultos al personal, ciclo rechazado, producción al PMP real y recálculo asíncrono tras subir el tomate.
  - `ModularityTests`: `recipes` depende de seis módulos y nadie depende de él; `catalog` solo añade `masterdata`.
- Arranque real en `dev`: 18 migraciones, seis cargadores de datos demo, ocho escandallos con coste, alérgenos en la carta pública y salsa producida en stock.
- `npm run build` y `npm run lint`: sin errores (el frontend no cambia en esta fase).
