package com.barandgo.crm.controller;

import com.barandgo.crm.dto.auth.AuthResponseDTO;
import com.barandgo.crm.dto.auth.LoginRequestDTO;
import com.barandgo.crm.dto.auth.RegisterRequestDTO;
import com.barandgo.crm.dto.user.UserResponseDTO;
import com.barandgo.crm.security.CurrentUser;
import com.barandgo.crm.service.AuthService;
import com.barandgo.crm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        return ResponseEntity.ok(authService.login(loginRequestDTO));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        return new ResponseEntity<>(authService.register(registerRequestDTO), HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getUserById(CurrentUser.from(jwt).id()));
    }
}
