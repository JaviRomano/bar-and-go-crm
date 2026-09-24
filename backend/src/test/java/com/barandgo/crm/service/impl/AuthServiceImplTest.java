package com.barandgo.crm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
import com.barandgo.crm.service.UserService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void loginReturnsTokenForValidCredentials() {
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("secreta1", "hash")).thenReturn(true);
        when(tokenService.generateToken(3L, "ana@example.com", UserRole.CUSTOMER)).thenReturn("token");
        when(tokenService.getExpirationSeconds()).thenReturn(7200L);

        AuthResponseDTO response = authService.login(login("ana@example.com", "secreta1"));

        assertThat(response.getAccessToken()).isEqualTo("token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(7200L);
        assertThat(response.getUser().getId()).isEqualTo(3L);
    }

    @Test
    void loginFailsWithWrongPassword() {
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("incorrecta", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(login("ana@example.com", "incorrecta")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void loginFailsWithSameMessageForUnknownEmail() {
        when(userRepository.findByEmail("nadie@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(login("nadie@example.com", "secreta1")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void registerAlwaysCreatesCustomer() {
        UserResponseDTO created = new UserResponseDTO();
        created.setId(8L);
        created.setEmail("nuevo@example.com");
        created.setRole(UserRole.CUSTOMER);
        when(userService.createUser(any(UserRequestDTO.class))).thenReturn(created);
        when(tokenService.generateToken(eq(8L), anyString(), eq(UserRole.CUSTOMER))).thenReturn("token");

        RegisterRequestDTO register = new RegisterRequestDTO();
        register.setName("Nuevo");
        register.setEmail("nuevo@example.com");
        register.setPassword("secreta1");
        register.setPhone("+34600000000");

        authService.register(register);

        ArgumentCaptor<UserRequestDTO> captor = ArgumentCaptor.forClass(UserRequestDTO.class);
        verify(userService).createUser(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    private static LoginRequestDTO login(String email, String password) {
        LoginRequestDTO login = new LoginRequestDTO();
        login.setEmail(email);
        login.setPassword(password);
        return login;
    }

    private static User user() {
        User user = new User();
        user.setId(3L);
        user.setName("Ana");
        user.setEmail("ana@example.com");
        user.setPassword("hash");
        user.setRole(UserRole.CUSTOMER);
        return user;
    }
}
