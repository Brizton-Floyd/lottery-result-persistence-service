package com.floyd.lottoptions.datamodels;

import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class UserRequest {
    private String sessionId;
    private String targetTier;
    private int numberOfTickets;
    private GenerationStrategy generationStrategy;
    private BigDecimal budget;
    private String lotteryConfigId;
    private Map<String, Object> preferences;
    private LocalDateTime timestamp;
    
    public enum GenerationStrategy {
        PATTERN_BASED,
        FREQUENCY_BASED,
        HYBRID,
        RANDOM_WEIGHTED,
        QUALITY_EVALUATION,
        BALANCED,
        CONSERVATIVE,
        AGGRESSIVE
    }
}