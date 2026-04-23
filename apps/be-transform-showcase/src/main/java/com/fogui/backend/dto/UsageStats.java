package com.fogui.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UsageStats {
    private CurrentPeriod currentPeriod;
    private List<DailyUsage> history;

    @Data
    @Builder
    public static class CurrentPeriod {
        private int transforms;
        private int quota;
        private int remaining;
    }

    @Data
    @Builder
    public static class DailyUsage {
        private String date;
        private int transforms;
    }
}
