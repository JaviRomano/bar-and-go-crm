package com.barandgo.crm.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.barandgo.crm.config.JwtConfig;
import com.barandgo.crm.enums.UserRole;

class TokenServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("test-only-hmac-key-bar-and-go-crm-32b".getBytes());
    private static final String OTHER_SECRET = Base64.getEncoder()
            .encodeToString("another-hmac-key-used-to-forge-tokens".getBytes());

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void generatedTokenCarriesIdentityAndRole() {
        JwtProperties properties = properties(SECRET, "bar-and-go-crm");
        SecretKey key = jwtConfig.jwtSecretKey(properties);
        TokenService tokenService = new TokenService(jwtConfig.jwtEncoder(key), properties);

        String token = tokenService.generateToken(7L, "ana@example.com", UserRole.CUSTOMER);
        Jwt jwt = jwtConfig.jwtDecoder(key, properties).decode(token);

        CurrentUser currentUser = CurrentUser.from(jwt);
        assertThat(currentUser.id()).isEqualTo(7L);
        assertThat(currentUser.email()).isEqualTo("ana@example.com");
        assertThat(currentUser.role()).isEqualTo(UserRole.CUSTOMER);
        assertThat(currentUser.isAdmin()).isFalse();
        assertThat(tokenService.getExpirationSeconds()).isEqualTo(Duration.ofMinutes(15).toSeconds());
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        JwtProperties forgerProperties = properties(OTHER_SECRET, "bar-and-go-crm");
        SecretKey forgerKey = jwtConfig.jwtSecretKey(forgerProperties);
        String forgedToken = new TokenService(jwtConfig.jwtEncoder(forgerKey), forgerProperties)
                .generateToken(1L, "admin@bar.com", UserRole.ADMIN);

        JwtProperties properties = properties(SECRET, "bar-and-go-crm");
        JwtDecoder decoder = jwtConfig.jwtDecoder(jwtConfig.jwtSecretKey(properties), properties);

        assertThatThrownBy(() -> decoder.decode(forgedToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        JwtProperties otherIssuer = properties(SECRET, "otra-aplicacion");
        SecretKey key = jwtConfig.jwtSecretKey(otherIssuer);
        String token = new TokenService(jwtConfig.jwtEncoder(key), otherIssuer)
                .generateToken(1L, "admin@bar.com", UserRole.ADMIN);

        JwtProperties properties = properties(SECRET, "bar-and-go-crm");
        JwtDecoder decoder = jwtConfig.jwtDecoder(key, properties);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsSecretShorterThan256Bits() {
        String shortSecret = Base64.getEncoder().encodeToString("demasiado-corta".getBytes());

        assertThatThrownBy(() -> jwtConfig.jwtSecretKey(properties(shortSecret, "bar-and-go-crm")))
                .isInstanceOf(IllegalStateException.class);
    }

    private static JwtProperties properties(String secret, String issuer) {
        return new JwtProperties(secret, Duration.ofMinutes(15), issuer);
    }
}
