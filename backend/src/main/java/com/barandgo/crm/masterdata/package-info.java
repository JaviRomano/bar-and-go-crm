/**
 * Datos maestros de compras: unidades de medida, proveedores y categorías de artículo.
 *
 * <p>El paquete raíz es la API del módulo; lo que está en {@code internal} no se usa desde fuera.
 */
@ApplicationModule(displayName = "Datos maestros", allowedDependencies = {"shared"})
package com.barandgo.crm.masterdata;

import org.springframework.modulith.ApplicationModule;
