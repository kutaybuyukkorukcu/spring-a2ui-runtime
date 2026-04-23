package com.fogui.backend.controller;

import com.fogui.backend.dto.AuthResponse;
import com.fogui.backend.dto.LoginRequest;
import com.fogui.backend.dto.RegisterRequest;
import com.fogui.backend.entity.User;
import com.fogui.backend.entity.UserRole;
import com.fogui.backend.repository.UserRepository;
import com.fogui.backend.security.ApiKeyUserDetails;
import com.fogui.backend.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication controller for user registration and login.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication (Reference Optional)", description = "Optional reference-server account registration and login endpoints")
public class AuthController {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;

        /**
         * Register a new user.
         */
        @PostMapping("/register")
        @Operation(summary = "Register user", description = "Creates a new user account and returns an access token")
        public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
                // Check if email already exists
                if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of("error", "Email already registered"));
                }

                // Create new user
                User user = User.builder()
                                .email(request.getEmail().toLowerCase())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .role(UserRole.FREE)
                                .monthlyQuota(UserRole.FREE.getMonthlyQuota())
                                .build();

                user = userRepository.save(user);
                log.info("New user registered: {}", user.getEmail());

                // Generate JWT and return
                String token = jwtService.generateToken(user);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(AuthResponse.from(token, user));
        }

        /**
         * Login with email and password.
         */
        @PostMapping("/login")
        @Operation(summary = "Login user", description = "Authenticates credentials and returns an access token")
        public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
                var userOpt = userRepository.findByEmail(request.getEmail().toLowerCase())
                                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                                .filter(User::getActive);

                if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        String token = jwtService.generateToken(user);
                        log.info("User logged in: {}", user.getEmail());
                        return ResponseEntity.ok(AuthResponse.from(token, user));
                }

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "Invalid email or password"));
        }

        /**
         * Get current user info (requires JWT auth).
         */
        @GetMapping("/me")
        @Operation(summary = "Get current user", description = "Returns profile details for the authenticated user")
        public ResponseEntity<?> me(@AuthenticationPrincipal ApiKeyUserDetails userDetails) {
                if (userDetails == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of("error", "Not authenticated"));
                }

                User user = userDetails.getUser();
                return ResponseEntity.ok(AuthResponse.UserInfo.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .role(user.getRole())
                                .monthlyQuota(user.getMonthlyQuota())
                                .usedThisMonth(user.getUsedThisMonth())
                                .build());
        }
}
