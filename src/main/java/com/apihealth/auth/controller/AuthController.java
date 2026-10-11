package com.apihealth.auth.controller;

import com.apihealth.auth.dto.*;
import com.apihealth.auth.service.AuthService;
import com.apihealth.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@Tag(name = "Authentication", description = "User registration, login, and profile endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout (client-side token invalidation)",
            description = "With stateless JWT, logout is handled client-side by discarding the token. " +
                    "This endpoint confirms the intent. For true server-side invalidation, " +
                    "a token blocklist backed by Redis would be needed.")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully. Discard the token on the client side.", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
        UserResponse user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
