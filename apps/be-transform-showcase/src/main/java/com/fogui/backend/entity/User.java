package com.fogui.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * User entity representing a FogUI platform user.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.FREE;

    @Column(name = "monthly_quota", nullable = false)
    @Builder.Default
    private Integer monthlyQuota = UserRole.FREE.getMonthlyQuota();

    @Column(name = "used_this_month", nullable = false)
    @Builder.Default
    private Integer usedThisMonth = 0;

    @Column(name = "quota_reset_date", nullable = false)
    @Builder.Default
    private LocalDate quotaResetDate = LocalDate.now();

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Check if user has remaining quota for this month.
     */
    public boolean hasQuota() {
        resetQuotaIfNeeded();
        return monthlyQuota < 0 || usedThisMonth < monthlyQuota; // -1 means unlimited
    }

    /**
     * Increment usage counter.
     */
    public void incrementUsage() {
        resetQuotaIfNeeded();
        this.usedThisMonth++;
    }

    /**
     * Reset quota if we're in a new month.
     */
    private void resetQuotaIfNeeded() {
        LocalDate today = LocalDate.now();
        if (today.getMonthValue() != quotaResetDate.getMonthValue()
                || today.getYear() != quotaResetDate.getYear()) {
            this.usedThisMonth = 0;
            this.quotaResetDate = today;
        }
    }
}
