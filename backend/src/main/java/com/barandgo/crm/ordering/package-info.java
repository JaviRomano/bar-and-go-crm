/**
 * Pedidos para recoger.
 *
 * <p>El paquete raíz es la API del módulo; lo que está en {@code internal} no se usa desde fuera.
 */
@ApplicationModule(displayName = "Pedidos", allowedDependencies = {"shared", "identity", "catalog"})
package com.barandgo.crm.ordering;

import org.springframework.modulith.ApplicationModule;
