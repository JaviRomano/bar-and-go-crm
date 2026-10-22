package com.barandgo.crm.identity;


public interface AuthService {
    
    AuthResponseDTO login(LoginRequestDTO loginRequestDTO);
    
    AuthResponseDTO register(RegisterRequestDTO registerRequestDTO);
}
