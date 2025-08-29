package com.floyd.lottoptions.datamodels;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class GeneratedTicketSet {
    private String sessionId;
    private List<LotteryTicket> tickets;
    private GenerationMetadata generationDetails;
    private QualityMetrics qualityMetrics;
    private List<String> patternsUsed;
    private ExpectedPerformance expectedPerformance;
    
    @Data
    public static class GenerationMetadata {
        private LocalDateTime generatedAt;
        private String generationStrategy;
        private String targetTier;
        private int requestedTickets;
        private String lotteryConfigId;
    }
    
    @Data
    public static class QualityMetrics {
        private double diversityScore;
        private double patternCoverageScore;
        private double expectedHitRate;
        private Map<String, Double> tierProbabilities;
    }
    
    @Data
    public static class ExpectedPerformance {
        private Map<String, Double> tierExpectations;
        private double overallConfidence;
        private String recommendationLevel;
    }
    
    @Data
    public static class LotteryTicket {
        private String ticketId;
        private List<Integer> numbers;
        private String patternUsed;
        private double confidenceScore;
        private Map<String, Object> metadata;
    }
}