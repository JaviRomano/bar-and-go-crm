package com.barandgo.crm;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Estructura de módulos (Spring Modulith): sin ciclos, sin acceso a tipos internos de otro módulo
 * y solo las dependencias declaradas en cada package-info.java.
 */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(BarAndGoCrmApplication.class);

    @Test
    void modulesRespectTheirBoundaries() {
        modules.verify();
    }

    // Diagramas C4 y lienzos de cada módulo en target/spring-modulith-docs
    @Test
    void writeDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
