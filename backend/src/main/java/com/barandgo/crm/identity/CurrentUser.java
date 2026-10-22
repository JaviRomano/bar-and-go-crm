package com.barandgo.crm.identity;

import com.barandgo.crm.identity.internal.TokenService;
import org.springframework.security.oauth2.jwt.Jwt;


/**
 * Identidad del usuario autenticado, extraída de los claims del token.
 */
public record CurrentUser(Long id, String email, UserRole role) {

    public static CurrentUser from(Jwt jwt) {
        Number userId = jwt.getClaim(TokenService.CLAIM_USER_ID);
        String role = jwt.getClaimAsString(TokenService.CLAIM_ROLE);
        return new CurrentUser(userId.longValue(), jwt.getSubject(), UserRole.valueOf(role));
    }

    /** Personal del local (ADMIN, MANAGER o STAFF): puede ver pedidos y reservas de cualquier cliente. */
    public boolean isBackOffice() {
        return role.isBackOffice();
    }
}
