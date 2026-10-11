package com.apihealth.auth.service;

import com.apihealth.auth.dto.*;
import com.apihealth.auth.security.JwtTokenProvider;
import com.apihealth.common.exception.ApiException;
import com.apihealth.common.exception.DuplicateResourceException;
import com.apihealth.user.entity.User;
import com.apihealth.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        User user = new User(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );
        user = userRepository.save(user);

        String token = tokenProvider.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!user.isEnabled()) {
            throw new ApiException("Account is disabled", HttpStatus.FORBIDDEN);
        }

        String token = tokenProvider.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    /**
     * Returns the User entity for the given email. Used internally for ownership checks.
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }
}
