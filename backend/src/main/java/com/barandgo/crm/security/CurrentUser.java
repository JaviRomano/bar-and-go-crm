package com.barandgo.crm.security;

import org.springframework.security.oauth2.jwt.Jwt;

import com.barandgo.crm.enums.UserRole;

/**
 * Identidad del usuario autenticado, extraída de los claims del token.
 */
public record CurrentUser(Long id, String email, UserRole role) {

    public static CurrentUser from(Jwt jwt) {
        Number userId = jwt.getClaim(TokenService.CLAIM_USER_ID);
        String role = jwt.getClaimAsString(TokenService.CLAIM_ROLE);
        return new CurrentUser(userId.longValue(), jwt.getSubject(), UserRole.valueOf(role));
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
