package com.barandgo.crm.shared;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Activa la ejecución asíncrona que usan los {@code @ApplicationModuleListener}: cada uno corre después
 * del commit del emisor, en su propia transacción y en otro hilo, de modo que un fallo en el receptor
 * no afecta a la operación que publicó el evento. Con spring.threads.virtual.enabled los hilos son
 * virtuales.
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {
}
