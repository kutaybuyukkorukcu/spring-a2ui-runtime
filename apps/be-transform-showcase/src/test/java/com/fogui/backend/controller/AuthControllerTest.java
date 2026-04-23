package com.fogui.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fogui.backend.dto.LoginRequest;
import com.fogui.backend.dto.RegisterRequest;
import com.fogui.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Tests user registration, login, and profile endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("should register new user successfully")
        void shouldRegisterNewUserSuccessfully() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("newuser@example.com");
            request.setPassword("securePassword123");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.user.email").value("newuser@example.com"))
                    .andExpect(jsonPath("$.user.role").value("FREE"));
        }

        @Test
        @DisplayName("should return 409 for duplicate email")
        void shouldReturn409ForDuplicateEmail() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("duplicate@example.com");
            request.setPassword("password123");

            // First registration
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Second registration with same email
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Email already registered"));
        }

        @Test
        @DisplayName("should handle case-insensitive email")
        void shouldHandleCaseInsensitiveEmail() throws Exception {
            RegisterRequest request1 = new RegisterRequest();
            request1.setEmail("User@Example.com");
            request1.setPassword("password123");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.email").value("user@example.com"));

            // Same email different case should conflict
            RegisterRequest request2 = new RegisterRequest();
            request2.setEmail("USER@EXAMPLE.COM");
            request2.setPassword("password456");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            RegisterRequest request = new RegisterRequest();
            // Missing email and password

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("should login with valid credentials")
        void shouldLoginWithValidCredentials() throws Exception {
            // First register
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEmail("logintest@example.com");
            registerRequest.setPassword("myPassword123");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated());

            // Then login
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail("logintest@example.com");
            loginRequest.setPassword("myPassword123");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.user.email").value("logintest@example.com"));
        }

        @Test
        @DisplayName("should return 401 for invalid password")
        void shouldReturn401ForInvalidPassword() throws Exception {
            // Register
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEmail("wrongpass@example.com");
            registerRequest.setPassword("correctPassword");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated());

            // Login with wrong password
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail("wrongpass@example.com");
            loginRequest.setPassword("wrongPassword");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid email or password"));
        }

        @Test
        @DisplayName("should return 401 for non-existent user")
        void shouldReturn401ForNonExistentUser() throws Exception {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail("nonexistent@example.com");
            loginRequest.setPassword("somePassword");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class Me {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return current user when authenticated")
        void shouldReturnCurrentUserWhenAuthenticated() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("me@example.com");
            request.setPassword("securePassword123");

            String token = objectMapper.readTree(mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("token").asText();

            mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.role").value("FREE"));
        }
    }
}
