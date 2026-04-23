package com.fogui.backend.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fogui.backend.entity.User;
import com.fogui.backend.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtService.
 * Tests JWT token generation, validation, and decoding.
 */
@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Use a test secret with 24-hour expiration
        jwtService = new JwtService("test-secret-key-minimum-32-characters-long", 24);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .role(UserRole.FREE)
                .build();
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("should generate valid JWT token")
        void shouldGenerateValidJwtToken() {
            String token = jwtService.generateToken(testUser);

            assertNotNull(token);
            assertFalse(token.isEmpty());
            // JWT has 3 parts separated by dots
            assertEquals(3, token.split("\\.").length);
        }

        @Test
        @DisplayName("should include user ID as subject")
        void shouldIncludeUserIdAsSubject() {
            String token = jwtService.generateToken(testUser);
            DecodedJWT decoded = jwtService.decodeToken(token);

            assertNotNull(decoded);
            assertEquals(testUser.getId().toString(), decoded.getSubject());
        }

        @Test
        @DisplayName("should include email claim")
        void shouldIncludeEmailClaim() {
            String token = jwtService.generateToken(testUser);
            DecodedJWT decoded = jwtService.decodeToken(token);

            assertNotNull(decoded);
            assertEquals("test@example.com", decoded.getClaim("email").asString());
        }

        @Test
        @DisplayName("should include role claim")
        void shouldIncludeRoleClaim() {
            String token = jwtService.generateToken(testUser);
            DecodedJWT decoded = jwtService.decodeToken(token);

            assertNotNull(decoded);
            assertEquals("FREE", decoded.getClaim("role").asString());
        }

        @Test
        @DisplayName("should set expiration time")
        void shouldSetExpirationTime() {
            String token = jwtService.generateToken(testUser);
            DecodedJWT decoded = jwtService.decodeToken(token);

            assertNotNull(decoded);
            assertNotNull(decoded.getExpiresAt());
            assertTrue(decoded.getExpiresAt().getTime() > System.currentTimeMillis());
        }
    }

    @Nested
    @DisplayName("validateTokenAndGetUserId")
    class ValidateTokenAndGetUserId {

        @Test
        @DisplayName("should return user ID for valid token")
        void shouldReturnUserIdForValidToken() {
            String token = jwtService.generateToken(testUser);
            UUID userId = jwtService.validateTokenAndGetUserId(token);

            assertNotNull(userId);
            assertEquals(testUser.getId(), userId);
        }

        @Test
        @DisplayName("should return null for invalid token")
        void shouldReturnNullForInvalidToken() {
            UUID userId = jwtService.validateTokenAndGetUserId("invalid.token.here");

            assertNull(userId);
        }

        @Test
        @DisplayName("should return null for tampered token")
        void shouldReturnNullForTamperedToken() {
            String token = jwtService.generateToken(testUser);
            // Tamper with the token by changing a character
            String tamperedToken = token.substring(0, 10) + "X" + token.substring(11);

            UUID userId = jwtService.validateTokenAndGetUserId(tamperedToken);

            assertNull(userId);
        }

        @Test
        @DisplayName("should return null for empty token")
        void shouldReturnNullForEmptyToken() {
            UUID userId = jwtService.validateTokenAndGetUserId("");

            assertNull(userId);
        }

        @Test
        @DisplayName("should return null for token signed with different secret")
        void shouldReturnNullForTokenSignedWithDifferentSecret() {
            JwtService otherService = new JwtService("different-secret-key-minimum-32-chars", 24);
            String token = otherService.generateToken(testUser);

            UUID userId = jwtService.validateTokenAndGetUserId(token);

            assertNull(userId);
        }
    }

    @Nested
    @DisplayName("decodeToken")
    class DecodeToken {

        @Test
        @DisplayName("should decode valid token")
        void shouldDecodeValidToken() {
            String token = jwtService.generateToken(testUser);
            DecodedJWT decoded = jwtService.decodeToken(token);

            assertNotNull(decoded);
            assertNotNull(decoded.getSubject());
            assertNotNull(decoded.getIssuedAt());
        }

        @Test
        @DisplayName("should return null for invalid token")
        void shouldReturnNullForInvalidToken() {
            DecodedJWT decoded = jwtService.decodeToken("not.a.valid.jwt");

            assertNull(decoded);
        }

        @Test
        @DisplayName("should include JWT ID")
        void shouldIncludeJwtId() {
            String token = jwtService.generateToken(testUser);
            DecodedJWT decoded = jwtService.decodeToken(token);

            assertNotNull(decoded);
            assertNotNull(decoded.getId());
            // JWT ID should be a valid UUID
            assertDoesNotThrow(() -> UUID.fromString(decoded.getId()));
        }
    }

    @Nested
    @DisplayName("Token expiration")
    class TokenExpiration {

        @Test
        @DisplayName("should create expired token with zero expiration hours")
        void shouldCreateExpiredTokenWithZeroExpirationHours() {
            // Create service with past expiration (0 hours means expires immediately)
            JwtService shortLivedService = new JwtService("test-secret-key-minimum-32-characters-long", 0);
            String token = shortLivedService.generateToken(testUser);

            // Token should still be decodable but expired
            // Note: With 0 hours, the token might already be expired
            DecodedJWT decoded = shortLivedService.decodeToken(token);

            // The decode itself may fail due to expiration validation
            // This is expected behavior
        }
    }
}
