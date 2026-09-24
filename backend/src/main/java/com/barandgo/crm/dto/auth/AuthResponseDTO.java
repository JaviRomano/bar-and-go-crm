package com.barandgo.crm.dto.auth;

import com.barandgo.crm.dto.user.UserResponseDTO;

public class AuthResponseDTO {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserResponseDTO user;

    public AuthResponseDTO() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * Segundos hasta la expiración del token.
     */
    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserResponseDTO getUser() {
        return user;
    }

    public void setUser(UserResponseDTO user) {
        this.user = user;
    }
}
