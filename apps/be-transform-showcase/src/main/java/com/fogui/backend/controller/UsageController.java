package com.fogui.backend.controller;

import com.fogui.backend.dto.UsageStats;
import com.fogui.backend.entity.User;
import com.fogui.backend.security.ApiKeyUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Slf4j
@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
@Tag(
        name = "Usage (Reference Optional)",
        description = "Optional reference-server usage and quota endpoints")
public class UsageController {

    @GetMapping("/stats")
    @Operation(summary = "Get usage stats", description = "Returns current quota and usage information")
    public ResponseEntity<UsageStats> getStats(@AuthenticationPrincipal ApiKeyUserDetails userDetails) {
        User user = userDetails.getUser();

        UsageStats.CurrentPeriod currentPeriod = UsageStats.CurrentPeriod.builder()
                .transforms(user.getUsedThisMonth())
                .quota(user.getMonthlyQuota())
                .remaining(user.getMonthlyQuota() == -1 ? -1
                        : user.getMonthlyQuota() - user.getUsedThisMonth())
                .build();

        // Mock history for now as we don't store daily granulatiy yet
        UsageStats.DailyUsage today = UsageStats.DailyUsage.builder()
                .date(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                .transforms(user.getUsedThisMonth()) // Just showing monthly total as today's usage for simplicity/mock
                .build();

        return ResponseEntity.ok(UsageStats.builder()
                .currentPeriod(currentPeriod)
                .history(Collections.singletonList(today))
                .build());
    }
}
