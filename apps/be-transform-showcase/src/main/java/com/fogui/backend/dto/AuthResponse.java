package com.fogui.backend.dto;

import com.fogui.backend.entity.User;
import com.fogui.backend.entity.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private String token;
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private UUID id;
        private String email;
        private UserRole role;
        private int monthlyQuota;
        private int usedThisMonth;
    }

    public static AuthResponse from(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .user(UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .monthlyQuota(user.getMonthlyQuota())
                        .usedThisMonth(user.getUsedThisMonth())
                        .build())
                .build();
    }
}
