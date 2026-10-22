package com.barandgo.crm.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.barandgo.crm.enums.UserRole;
import com.barandgo.crm.security.TokenService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = UserRole.ADMIN.name();
    private static final String MANAGER = UserRole.MANAGER.name();
    private static final String STAFF = UserRole.STAFF.name();

    /**
     * ADMIN incluye los permisos de MANAGER y MANAGER los de STAFF, así que hasRole(STAFF)
     * admite a todo el personal. CUSTOMER queda fuera de la jerarquía.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(ADMIN).implies(MANAGER)
                .role(MANAGER).implies(STAFF)
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // API stateless con token Bearer en cabecera: sin cookies de sesión no aplica CSRF
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                // Carta pública
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/product-categories/**").permitAll()
                .requestMatchers("/api/products/**", "/api/product-categories/**").hasRole(MANAGER)
                // Usuarios: el servicio impide que un MANAGER gestione ADMIN o MANAGER
                .requestMatchers("/api/users/**").hasRole(MANAGER)
                // Pedidos: el cliente crea y consulta los suyos; la propiedad se comprueba en el servicio
                .requestMatchers(HttpMethod.POST, "/api/orders").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/orders/me", "/api/orders/{id:\\d+}").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasRole(MANAGER)
                .requestMatchers("/api/orders/**").hasRole(STAFF)
                // Reservas: el cliente crea, consulta y cancela las suyas
                .requestMatchers(HttpMethod.POST, "/api/reservations").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/reservations/me").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/reservations/{id:\\d+}/cancel").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/reservations/**").hasRole(MANAGER)
                .requestMatchers("/api/reservations/**").hasRole(STAFF)
                // Maestros de compras: el personal consulta, el encargado mantiene
                .requestMatchers(HttpMethod.GET, "/api/suppliers/**", "/api/item-categories/**").hasRole(STAFF)
                .requestMatchers("/api/suppliers/**", "/api/item-categories/**").hasRole(MANAGER)
                // Unidades: configuración; el encargado consulta, solo ADMIN modifica
                .requestMatchers(HttpMethod.GET, "/api/units/**").hasRole(MANAGER)
                .requestMatchers("/api/units/**").hasRole(ADMIN)
                // Lo que no tenga regla explícita queda cerrado, incluidos endpoints nuevos sin configurar
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    // Traduce el claim "role" del token a la autoridad ROLE_<rol> que usa hasRole()
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(TokenService.CLAIM_ROLE);
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
