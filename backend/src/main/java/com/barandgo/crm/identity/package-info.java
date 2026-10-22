/**
 * Usuarios, roles, autenticación JWT y reglas de acceso.
 *
 * <p>El paquete raíz es la API del módulo; lo que está en {@code internal} no se usa desde fuera.
 */
@ApplicationModule(displayName = "Identidad y acceso", allowedDependencies = {"shared"})
package com.barandgo.crm.identity;

import org.springframework.modulith.ApplicationModule;
