package com.fogui.backend.security;

import com.fogui.backend.entity.User;
import com.fogui.backend.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApiKeyUserDetails.
 * Tests UserDetails implementation for authenticated reference-server users.
 */
@DisplayName("ApiKeyUserDetails")
class ApiKeyUserDetailsTest {

    private User user;
    private ApiKeyUserDetails userDetails;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(UserRole.FREE)
                .active(true)
                .build();
        userDetails = new ApiKeyUserDetails(user);
    }

    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        @DisplayName("should return wrapped user")
        void shouldReturnWrappedUser() {
            assertEquals(user, userDetails.getUser());
        }
    }

    @Nested
    @DisplayName("getAuthorities")
    class GetAuthorities {

        @Test
        @DisplayName("should return ROLE_FREE for free user")
        void shouldReturnRoleFreeForFreeUser() {
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

            assertEquals(1, authorities.size());
            assertTrue(authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_FREE")));
        }

        @Test
        @DisplayName("should return ROLE_PRO for pro user")
        void shouldReturnRoleProForProUser() {
            user.setRole(UserRole.PRO);
            userDetails = new ApiKeyUserDetails(user);

            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

            assertEquals(1, authorities.size());
            assertTrue(authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_PRO")));
        }
    }

    @Nested
    @DisplayName("getUsername")
    class GetUsername {

        @Test
        @DisplayName("should return user email")
        void shouldReturnUserEmail() {
            assertEquals("test@example.com", userDetails.getUsername());
        }
    }

    @Nested
    @DisplayName("getPassword")
    class GetPassword {

        @Test
        @DisplayName("should return null for token-based auth")
        void shouldReturnNullForApiKeyAuth() {
            assertNull(userDetails.getPassword());
        }
    }

    @Nested
    @DisplayName("Account Status")
    class AccountStatus {

        @Test
        @DisplayName("isAccountNonExpired should return true")
        void isAccountNonExpiredShouldReturnTrue() {
            assertTrue(userDetails.isAccountNonExpired());
        }

        @Test
        @DisplayName("isCredentialsNonExpired should return true")
        void isCredentialsNonExpiredShouldReturnTrue() {
            assertTrue(userDetails.isCredentialsNonExpired());
        }

        @Test
        @DisplayName("isAccountNonLocked should return true for active user")
        void isAccountNonLockedShouldReturnTrueForActiveUser() {
            assertTrue(userDetails.isAccountNonLocked());
        }

        @Test
        @DisplayName("isAccountNonLocked should return false for inactive user")
        void isAccountNonLockedShouldReturnFalseForInactiveUser() {
            user.setActive(false);
            userDetails = new ApiKeyUserDetails(user);

            assertFalse(userDetails.isAccountNonLocked());
        }

        @Test
        @DisplayName("isEnabled should return true for active user")
        void isEnabledShouldReturnTrueForActiveUser() {
            assertTrue(userDetails.isEnabled());
        }

        @Test
        @DisplayName("isEnabled should return false for inactive user")
        void isEnabledShouldReturnFalseForInactiveUser() {
            user.setActive(false);
            userDetails = new ApiKeyUserDetails(user);

            assertFalse(userDetails.isEnabled());
        }
    }
}
