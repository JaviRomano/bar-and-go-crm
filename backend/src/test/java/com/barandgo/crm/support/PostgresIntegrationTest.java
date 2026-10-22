package com.barandgo.crm.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test de integración contra el PostgreSQL de la clase (campo estático con {@code @Container} y
 * {@code @ServiceConnection}). Se omite sin Docker.
 *
 * <p>El orden importa: Testcontainers va antes que Spring para que, al terminar la clase, primero se cierre
 * el contexto ({@code @DirtiesContext}) y después se pare el contenedor. Al revés, el contexto quedaría en
 * caché apuntando a una BD parada y al cerrar la JVM el registro de eventos de Modulith esperaría una
 * conexión hasta agotar el timeout.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
public @interface PostgresIntegrationTest {
}
