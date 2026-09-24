package com.barandgo.crm.service;

import com.barandgo.crm.dto.auth.AuthResponseDTO;
import com.barandgo.crm.dto.auth.LoginRequestDTO;
import com.barandgo.crm.dto.auth.RegisterRequestDTO;

public interface AuthService {
    
    AuthResponseDTO login(LoginRequestDTO loginRequestDTO);
    
    AuthResponseDTO register(RegisterRequestDTO registerRequestDTO);
}
