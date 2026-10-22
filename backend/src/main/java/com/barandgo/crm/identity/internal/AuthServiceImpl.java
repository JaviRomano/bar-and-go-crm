package com.barandgo.crm.identity.internal;

import com.barandgo.crm.identity.AuthResponseDTO;
import com.barandgo.crm.identity.LoginRequestDTO;
import com.barandgo.crm.identity.RegisterRequestDTO;
import com.barandgo.crm.identity.UserRequestDTO;
import com.barandgo.crm.identity.UserResponseDTO;
import com.barandgo.crm.identity.User;
import com.barandgo.crm.shared.UnauthorizedException;
import com.barandgo.crm.identity.AuthService;
import com.barandgo.crm.identity.UserService;
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

        return buildResponse(userService.registerCustomer(userRequestDTO));
    }

    private AuthResponseDTO buildResponse(UserResponseDTO user) {
        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(tokenService.generateToken(user.getId(), user.getEmail(), user.getRole()));
        response.setExpiresIn(tokenService.getExpirationSeconds());
        response.setUser(user);
        return response;
    }
}
