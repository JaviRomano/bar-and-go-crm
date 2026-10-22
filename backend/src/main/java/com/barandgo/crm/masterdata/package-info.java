/**
 * Datos maestros: unidades de medida y conversión, proveedores, categorías de artículo, tipos de IVA y
 * alérgenos.
 *
 * <p>El paquete raíz es la API del módulo; lo que está en {@code internal} no se usa desde fuera.
 */
@ApplicationModule(displayName = "Datos maestros", allowedDependencies = {"shared"})
package com.barandgo.crm.masterdata;

import org.springframework.modulith.ApplicationModule;
