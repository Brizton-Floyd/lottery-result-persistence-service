package com.floyd.lottoptions.datamodels;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class NumberFrequency {
    private Long id;
    private String lotteryConfigId;
    private Map<String, Double> frequencies; // {"1": 0.0285, "2": 0.0312, ...}
    private Integer totalDrawsAnalyzed;
    private LocalDateTime lastUpdated;
}