package com.barandgo.crm.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuración del token de acceso (prefijo app.jwt).
 *
 * @param secret     clave HMAC en Base64, mínimo 256 bits
 * @param expiration validez del token (ISO-8601, p. ej. PT2H)
 * @param issuer     valor del claim iss, se valida al decodificar
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull Duration expiration,
        @NotBlank String issuer) {
}
