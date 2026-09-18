package com.trizen.photoshare.controller;

import com.trizen.photoshare.dto.AuthDtos.AuthResponse;
import com.trizen.photoshare.dto.AuthDtos.LoginRequest;
import com.trizen.photoshare.dto.AuthDtos.RegisterRequest;
import com.trizen.photoshare.dto.AuthDtos.UserDto;
import com.trizen.photoshare.security.AppUserPrincipal;
import com.trizen.photoshare.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return authService.currentUser(principal);
    }
}
