package com.fogui.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fogui.backend.dto.UserProfile;
import com.fogui.backend.entity.User;
import com.fogui.backend.entity.UserRole;
import com.fogui.backend.repository.UserRepository;
import com.fogui.backend.security.JwtService;
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
 * Integration tests for UserController.
 * Tests user profile retrieval and update endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
        private JwtService jwtService;

    private User testUser;
        private String jwtToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .email("user-test@example.com")
                .passwordHash("hashed")
                .role(UserRole.FREE)
                .monthlyQuota(100)
                .build();
        testUser = userRepository.save(testUser);

        jwtToken = jwtService.generateToken(testUser);
    }

    @Nested
    @DisplayName("GET /api/user/profile")
    class GetProfile {

        @Test
        @DisplayName("should return user profile with authenticated user")
        void shouldReturnUserProfile() throws Exception {
            mockMvc.perform(get("/api/user/profile")
                                        .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
                    .andExpect(jsonPath("$.email").value("user-test@example.com"))
                    .andExpect(jsonPath("$.name").value("user-test@example.com"))
                    .andExpect(jsonPath("$.createdAt").exists());
        }

                @Test
                @DisplayName("should reject unauthenticated access")
                void shouldRejectUnauthenticatedAccess() throws Exception {
                        mockMvc.perform(get("/api/user/profile"))
                                        .andExpect(status().isForbidden());
                }
    }

    @Nested
    @DisplayName("PUT /api/user/profile")
    class UpdateProfile {

        @Test
        @DisplayName("should update email when valid new email provided")
        void shouldUpdateEmailWhenValid() throws Exception {
            UserProfile request = UserProfile.builder()
                    .email("newemail@example.com")
                    .build();

            mockMvc.perform(put("/api/user/profile")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("newemail@example.com"));
        }

        @Test
        @DisplayName("should return 409 when email already in use")
        void shouldReturn409WhenEmailInUse() throws Exception {
            // Create another user with target email
            User otherUser = User.builder()
                    .email("taken@example.com")
                    .passwordHash("hashed")
                    .role(UserRole.FREE)
                    .monthlyQuota(100)
                    .build();
            userRepository.save(otherUser);

            UserProfile request = UserProfile.builder()
                    .email("taken@example.com")
                    .build();

            mockMvc.perform(put("/api/user/profile")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should not update email when same as current")
        void shouldNotUpdateWhenSameEmail() throws Exception {
            UserProfile request = UserProfile.builder()
                    .email("user-test@example.com")
                    .build();

            mockMvc.perform(put("/api/user/profile")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user-test@example.com"));
        }

        @Test
        @DisplayName("should not update email when null")
        void shouldNotUpdateWhenEmailNull() throws Exception {
            UserProfile request = UserProfile.builder()
                    .name("Some Name")
                    .build();

            mockMvc.perform(put("/api/user/profile")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user-test@example.com"));
        }

        @Test
        @DisplayName("should not update email when blank")
        void shouldNotUpdateWhenEmailBlank() throws Exception {
            UserProfile request = UserProfile.builder()
                    .email("   ")
                    .build();

            mockMvc.perform(put("/api/user/profile")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user-test@example.com"));
        }
    }
}
