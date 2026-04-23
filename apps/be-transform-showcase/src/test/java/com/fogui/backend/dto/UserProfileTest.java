package com.fogui.backend.dto;

import com.fogui.backend.entity.User;
import com.fogui.backend.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserProfile DTO.
 * Tests factory method from User entity.
 */
@DisplayName("UserProfile")
class UserProfileTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(UserRole.FREE)
                .build();
        user.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("from(User)")
    class From {

        @Test
        @DisplayName("should map User to UserProfile correctly")
        void shouldMapUserToUserProfileCorrectly() {
            UserProfile profile = UserProfile.from(user);

            assertEquals(user.getId(), profile.getId());
            assertEquals(user.getEmail(), profile.getEmail());
            assertEquals(user.getCreatedAt(), profile.getCreatedAt());
        }

        @Test
        @DisplayName("should use email as name fallback")
        void shouldUseEmailAsNameFallback() {
            UserProfile profile = UserProfile.from(user);

            assertEquals(user.getEmail(), profile.getName());
        }

        @Test
        @DisplayName("should handle different emails")
        void shouldHandleDifferentEmails() {
            user.setEmail("another@example.com");

            UserProfile profile = UserProfile.from(user);

            assertEquals("another@example.com", profile.getEmail());
            assertEquals("another@example.com", profile.getName());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build UserProfile with all fields")
        void shouldBuildUserProfileWithAllFields() {
            UUID id = UUID.randomUUID();
            OffsetDateTime createdAt = OffsetDateTime.now();

            UserProfile profile = UserProfile.builder()
                    .id(id)
                    .email("custom@example.com")
                    .name("Custom Name")
                    .createdAt(createdAt)
                    .build();

            assertEquals(id, profile.getId());
            assertEquals("custom@example.com", profile.getEmail());
            assertEquals("Custom Name", profile.getName());
            assertEquals(createdAt, profile.getCreatedAt());
        }

        @Test
        @DisplayName("should allow null values")
        void shouldAllowNullValues() {
            UserProfile profile = UserProfile.builder()
                    .email(null)
                    .name(null)
                    .build();

            assertNull(profile.getEmail());
            assertNull(profile.getName());
        }
    }
}
