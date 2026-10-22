/**
 * Inventario: almacenes, libro de movimientos de stock (inmutable), saldos, valoración PMP, lotes,
 * recepción de albaranes, ajustes, traspasos, recuentos y sugerencia de reposición.
 *
 * <p>El paquete raíz es la API del módulo; lo que está en {@code internal} no se usa desde fuera.
 * Depende de compras (artículos y pedidos); compras no depende de inventario.
 */
@ApplicationModule(displayName = "Inventario",
        allowedDependencies = {"shared", "identity", "masterdata", "purchasing"})
package com.barandgo.crm.inventory;

import org.springframework.modulith.ApplicationModule;
