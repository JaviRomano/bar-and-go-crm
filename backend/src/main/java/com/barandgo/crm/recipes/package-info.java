/**
 * Escandallos: fichas técnicas de platos y elaboraciones, coste por ración, food cost, alérgenos y
 * producción de elaboraciones.
 *
 * <p>El paquete raíz es la API del módulo; lo que está en {@code internal} no se usa desde fuera.
 * Responde a la carta los alérgenos de cada producto (ProductAllergenSource) y escucha los cambios de
 * coste de compras para recalcular.
 */
@ApplicationModule(displayName = "Escandallos",
        allowedDependencies = {"shared", "identity", "masterdata", "purchasing", "catalog", "inventory"})
package com.barandgo.crm.recipes;

import org.springframework.modulith.ApplicationModule;
