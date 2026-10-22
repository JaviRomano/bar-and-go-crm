/**
 * Informes de gestión: valor de stock, food cost real frente a teórico, mermas, rotación, evolución de
 * precios de compra e ingeniería de menú. Solo lee: no tiene tablas propias.
 *
 * <p>El paquete raíz es la API del módulo; lo que está en {@code internal} no se usa desde fuera.
 */
@ApplicationModule(displayName = "Informes",
        allowedDependencies = {"shared", "identity", "masterdata", "purchasing", "catalog", "ordering", "recipes",
                "inventory", "costcontrol"})
package com.barandgo.crm.reporting;

import org.springframework.modulith.ApplicationModule;
