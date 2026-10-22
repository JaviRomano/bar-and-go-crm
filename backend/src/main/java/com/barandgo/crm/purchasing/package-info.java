/**
 * Compras: artículos de compra (materia prima y mercancía), precios por proveedor y pedidos a proveedor.
 *
 * <p>El paquete raíz es la API del módulo; lo que está en {@code internal} no se usa desde fuera.
 * Con los datos maestros se relaciona por id (con FK en base de datos), no con relaciones JPA.
 */
@ApplicationModule(displayName = "Compras", allowedDependencies = {"shared", "identity", "masterdata"})
package com.barandgo.crm.purchasing;

import org.springframework.modulith.ApplicationModule;
