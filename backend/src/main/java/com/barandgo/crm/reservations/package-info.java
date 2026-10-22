/**
 * Reservas de mesa.
 *
 * <p>El paquete raíz es la API del módulo; lo que está en {@code internal} no se usa desde fuera.
 */
@ApplicationModule(displayName = "Reservas", allowedDependencies = {"shared", "identity"})
package com.barandgo.crm.reservations;

import org.springframework.modulith.ApplicationModule;
