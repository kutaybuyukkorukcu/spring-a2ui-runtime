package com.fogui.backend.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for User entity.
 * Tests quota management and business logic.
 */
@DisplayName("User Entity")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(UserRole.FREE)
                .monthlyQuota(100)
                .usedThisMonth(0)
                .quotaResetDate(LocalDate.now())
                .build();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("should have FREE role by default")
        void shouldHaveFreeRoleByDefault() {
            User newUser = User.builder()
                    .email("new@example.com")
                    .passwordHash("hash")
                    .build();

            assertEquals(UserRole.FREE, newUser.getRole());
        }

        @Test
        @DisplayName("should have usedThisMonth as 0 by default")
        void shouldHaveZeroUsageByDefault() {
            User newUser = User.builder()
                    .email("new@example.com")
                    .passwordHash("hash")
                    .build();

            assertEquals(0, newUser.getUsedThisMonth());
        }

        @Test
        @DisplayName("should be active by default")
        void shouldBeActiveByDefault() {
            User newUser = User.builder()
                    .email("new@example.com")
                    .passwordHash("hash")
                    .build();

            assertTrue(newUser.getActive());
        }

        @Test
        @DisplayName("should have createdAt set")
        void shouldHaveCreatedAtSet() {
            User newUser = User.builder()
                    .email("new@example.com")
                    .passwordHash("hash")
                    .build();

            assertNotNull(newUser.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("hasQuota")
    class HasQuota {

        @Test
        @DisplayName("should return true when usage is less than quota")
        void shouldReturnTrueWhenUsageLessThanQuota() {
            user.setUsedThisMonth(50);
            user.setMonthlyQuota(100);

            assertTrue(user.hasQuota());
        }

        @Test
        @DisplayName("should return false when usage equals quota")
        void shouldReturnFalseWhenUsageEqualsQuota() {
            user.setUsedThisMonth(100);
            user.setMonthlyQuota(100);

            assertFalse(user.hasQuota());
        }

        @Test
        @DisplayName("should return false when usage exceeds quota")
        void shouldReturnFalseWhenUsageExceedsQuota() {
            user.setUsedThisMonth(150);
            user.setMonthlyQuota(100);

            assertFalse(user.hasQuota());
        }

        @Test
        @DisplayName("should return true for unlimited quota (-1)")
        void shouldReturnTrueForUnlimitedQuota() {
            user.setMonthlyQuota(-1);
            user.setUsedThisMonth(999999);

            assertTrue(user.hasQuota());
        }

        @Test
        @DisplayName("should return true when usage is zero")
        void shouldReturnTrueWhenUsageIsZero() {
            user.setUsedThisMonth(0);
            user.setMonthlyQuota(100);

            assertTrue(user.hasQuota());
        }
    }

    @Nested
    @DisplayName("incrementUsage")
    class IncrementUsage {

        @Test
        @DisplayName("should increase usedThisMonth by 1")
        void shouldIncreaseUsedThisMonthByOne() {
            user.setUsedThisMonth(10);

            user.incrementUsage();

            assertEquals(11, user.getUsedThisMonth());
        }

        @Test
        @DisplayName("should increment from zero")
        void shouldIncrementFromZero() {
            user.setUsedThisMonth(0);

            user.incrementUsage();

            assertEquals(1, user.getUsedThisMonth());
        }

        @Test
        @DisplayName("should increment multiple times")
        void shouldIncrementMultipleTimes() {
            user.setUsedThisMonth(0);

            user.incrementUsage();
            user.incrementUsage();
            user.incrementUsage();

            assertEquals(3, user.getUsedThisMonth());
        }
    }

    @Nested
    @DisplayName("Quota Reset Logic")
    class QuotaResetLogic {

        @Test
        @DisplayName("should reset quota when month changes")
        void shouldResetQuotaWhenMonthChanges() {
            // Set to previous month
            user.setQuotaResetDate(LocalDate.now().minusMonths(1));
            user.setUsedThisMonth(50);

            // hasQuota should trigger reset
            user.hasQuota();

            assertEquals(0, user.getUsedThisMonth());
            assertEquals(LocalDate.now(), user.getQuotaResetDate());
        }

        @Test
        @DisplayName("should reset quota when year changes")
        void shouldResetQuotaWhenYearChanges() {
            // Set to previous year, same month
            user.setQuotaResetDate(LocalDate.now().minusYears(1));
            user.setUsedThisMonth(75);

            user.hasQuota();

            assertEquals(0, user.getUsedThisMonth());
            assertEquals(LocalDate.now(), user.getQuotaResetDate());
        }

        @Test
        @DisplayName("should not reset quota when still in same month")
        void shouldNotResetQuotaWhenSameMonth() {
            user.setQuotaResetDate(LocalDate.now());
            user.setUsedThisMonth(50);

            user.hasQuota();

            assertEquals(50, user.getUsedThisMonth());
        }

        @Test
        @DisplayName("should reset on incrementUsage if month changed")
        void shouldResetOnIncrementIfMonthChanged() {
            user.setQuotaResetDate(LocalDate.now().minusMonths(1));
            user.setUsedThisMonth(99);

            user.incrementUsage();

            // Should have reset to 0 then incremented to 1
            assertEquals(1, user.getUsedThisMonth());
        }
    }

    @Nested
    @DisplayName("UserRole integration")
    class UserRoleIntegration {

        @Test
        @DisplayName("should use FREE role quota by default")
        void shouldUseFreeRoleQuotaByDefault() {
            User newUser = User.builder()
                    .email("new@example.com")
                    .passwordHash("hash")
                    .build();

            assertEquals(UserRole.FREE.getMonthlyQuota(), newUser.getMonthlyQuota());
        }
    }
}
