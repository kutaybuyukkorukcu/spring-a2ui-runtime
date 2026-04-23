package com.fogui.backend.controller;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UsageController.
 * Tests usage statistics endpoint for authenticated reference-server users.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("UsageController")
class UsageControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
                .email("usage-test@example.com")
                .passwordHash("hashed")
                .role(UserRole.FREE)
                .monthlyQuota(100)
                .usedThisMonth(25)
                .quotaResetDate(LocalDate.now())
                .build();
        testUser = userRepository.save(testUser);

            jwtToken = jwtService.generateToken(testUser);
    }

    @Nested
    @DisplayName("GET /api/usage/stats")
    class GetStats {

        @Test
        @DisplayName("should return usage stats with authenticated user")
        void shouldReturnUsageStats() throws Exception {
            mockMvc.perform(get("/api/usage/stats")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentPeriod.transforms").value(25))
                    .andExpect(jsonPath("$.currentPeriod.quota").value(100))
                    .andExpect(jsonPath("$.currentPeriod.remaining").value(75))
                    .andExpect(jsonPath("$.history").isArray())
                    .andExpect(jsonPath("$.history[0].date").exists())
                    .andExpect(jsonPath("$.history[0].transforms").value(25));
        }

        @Test
        @DisplayName("should calculate remaining quota correctly")
        void shouldCalculateRemainingQuotaCorrectly() throws Exception {
            // Update user usage
            testUser.setUsedThisMonth(80);
            userRepository.save(testUser);

            mockMvc.perform(get("/api/usage/stats")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentPeriod.transforms").value(80))
                    .andExpect(jsonPath("$.currentPeriod.remaining").value(20));
        }

        @Test
        @DisplayName("should return -1 remaining for unlimited quota")
        void shouldReturnMinusOneForUnlimitedQuota() throws Exception {
            // Set unlimited quota
            testUser.setMonthlyQuota(-1);
            userRepository.save(testUser);

            mockMvc.perform(get("/api/usage/stats")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentPeriod.quota").value(-1))
                    .andExpect(jsonPath("$.currentPeriod.remaining").value(-1));
        }

        @Test
        @DisplayName("should return current date in history")
        void shouldReturnCurrentDateInHistory() throws Exception {
            String today = LocalDate.now().toString();

            mockMvc.perform(get("/api/usage/stats")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.history[0].date").value(today));
        }

        @Test
        @DisplayName("should handle user with zero usage")
        void shouldHandleZeroUsage() throws Exception {
            testUser.setUsedThisMonth(0);
            userRepository.save(testUser);

            mockMvc.perform(get("/api/usage/stats")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentPeriod.transforms").value(0))
                    .andExpect(jsonPath("$.currentPeriod.remaining").value(100));
        }

        @Test
        @DisplayName("should reject unauthenticated requests")
        void shouldRejectUnauthenticatedRequests() throws Exception {
            mockMvc.perform(get("/api/usage/stats"))
                    .andExpect(status().isForbidden());
        }
    }
}
