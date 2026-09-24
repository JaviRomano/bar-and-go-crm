package com.barandgo.crm.service.impl;

import com.barandgo.crm.dto.auth.AuthResponseDTO;
import com.barandgo.crm.dto.auth.LoginRequestDTO;
import com.barandgo.crm.dto.auth.RegisterRequestDTO;
import com.barandgo.crm.dto.user.UserRequestDTO;
import com.barandgo.crm.dto.user.UserResponseDTO;
import com.barandgo.crm.entity.User;
import com.barandgo.crm.enums.UserRole;
import com.barandgo.crm.exception.UnauthorizedException;
import com.barandgo.crm.repository.UserRepository;
import com.barandgo.crm.security.TokenService;
import com.barandgo.crm.service.AuthService;
import com.barandgo.crm.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    // Mismo mensaje para email inexistente y contraseña errónea: no revela qué cuentas existen
    private static final String INVALID_CREDENTIALS = "Email o contraseña incorrectos";

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthServiceImpl(UserRepository userRepository, UserService userService,
                           PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO loginRequestDTO) {
        User user = userRepository.findByEmail(loginRequestDTO.getEmail())
                .filter(candidate -> passwordEncoder.matches(loginRequestDTO.getPassword(), candidate.getPassword()))
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));

        return buildResponse(UserResponseDTO.from(user));
    }

    @Override
    public AuthResponseDTO register(RegisterRequestDTO registerRequestDTO) {
        UserRequestDTO userRequestDTO = new UserRequestDTO();
        userRequestDTO.setName(registerRequestDTO.getName());
        userRequestDTO.setEmail(registerRequestDTO.getEmail());
        userRequestDTO.setPassword(registerRequestDTO.getPassword());
        userRequestDTO.setPhone(registerRequestDTO.getPhone());
        userRequestDTO.setRole(UserRole.CUSTOMER);

        return buildResponse(userService.createUser(userRequestDTO));
    }

    private AuthResponseDTO buildResponse(UserResponseDTO user) {
        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(tokenService.generateToken(user.getId(), user.getEmail(), user.getRole()));
        response.setExpiresIn(tokenService.getExpirationSeconds());
        response.setUser(user);
        return response;
    }
}
