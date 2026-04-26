package com.fogui.backend.entity;

import lombok.Getter;

/**
 * User roles with associated monthly transform quotas.
 */
@Getter
public enum UserRole {
    FREE(100), // 100 transforms/month (trial/free tier)
    PRO(10_000); // 10,000 transforms/month (paid tier)

    private final int monthlyQuota;

    UserRole(int monthlyQuota) {
        this.monthlyQuota = monthlyQuota;
    }
}
