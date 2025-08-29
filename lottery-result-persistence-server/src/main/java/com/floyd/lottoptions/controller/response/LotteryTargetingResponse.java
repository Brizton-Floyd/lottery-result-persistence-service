package com.floyd.lottoptions.controller.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LotteryTargetingResponse {
    
    @Data
    public static class ConfigurationSummary {
        private String id;
        private String name;
        private String numberRange;
        private int drawSize;
        private List<String> availableTiers;
        private LocalDateTime lastUpdated;
    }
    
    @Data
    public static class PatternSummary {
        private String lotteryConfigId;
        private String patternType;
        private int patternCount;
        private double averageEfficiency;
        private LocalDateTime lastAnalysis;
    }
    
    @Data
    public static class SessionSummary {
        private String sessionId;
        private String lotteryGame;
        private String targetTier;
        private int ticketsRequested;
        private String strategy;
        private LocalDateTime createdAt;
    }
}