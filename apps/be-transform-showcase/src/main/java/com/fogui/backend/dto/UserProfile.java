package com.fogui.backend.dto;

import com.fogui.backend.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class UserProfile {
    private UUID id;
    private String email;
    private String name;
    private OffsetDateTime createdAt;

    public static UserProfile from(User user) {
        return UserProfile.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getEmail()) // Fallback to email as name if name is missing in Entity, or add name to Entity
                                       // later
                .createdAt(user.getCreatedAt())
                .build();
    }
}
