/**
 * Control de costes: descuento de stock por venta (escuchando los eventos de pedidos), mermas y
 * consumo teórico frente a real.
 *
 * <p>El paquete raíz es la API del módulo; lo que está en {@code internal} no se usa desde fuera.
 */
@ApplicationModule(displayName = "Control de costes",
        allowedDependencies = {"shared", "identity", "masterdata", "purchasing", "catalog", "ordering", "recipes",
                "inventory"})
package com.barandgo.crm.costcontrol;

import org.springframework.modulith.ApplicationModule;
