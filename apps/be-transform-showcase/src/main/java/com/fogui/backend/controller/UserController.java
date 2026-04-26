package com.fogui.backend.controller;

import com.fogui.backend.dto.UserProfile;
import com.fogui.backend.entity.User;
import com.fogui.backend.repository.UserRepository;
import com.fogui.backend.security.ApiKeyUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User (Reference Optional)", description = "Optional reference-server user profile endpoints")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/profile")
    @Operation(summary = "Get profile", description = "Returns current user profile")
    public ResponseEntity<UserProfile> getProfile(@AuthenticationPrincipal ApiKeyUserDetails userDetails) {
        return ResponseEntity.ok(UserProfile.from(userDetails.getUser()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update profile", description = "Updates current user profile fields")
    public ResponseEntity<UserProfile> updateProfile(
            @AuthenticationPrincipal ApiKeyUserDetails userDetails,
            @RequestBody UserProfile request) {

        User user = userDetails.getUser();

        // Only allow updating allowed fields (e.g. name if we had it, or email with
        // verification)
        // For now, let's say we assume email update is allowed without verification for
        // MVP (or just reject it)
        // Actually, User entity doesn't have a 'name' field distinct from email in the
        // current definition.
        // So we might only be able to update email.

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equals(user.getEmail())) {

            if (userRepository.existsByEmail(request.getEmail())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, "Email already in use");
            }

            user.setEmail(request.getEmail());
        }

        userRepository.save(user);

        return ResponseEntity.ok(UserProfile.from(user));
    }
}
