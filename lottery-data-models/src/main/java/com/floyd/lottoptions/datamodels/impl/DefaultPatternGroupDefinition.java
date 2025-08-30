package com.floyd.lottoptions.datamodels.impl;

import com.floyd.lottoptions.datamodels.PatternGroupDefinition;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DefaultPatternGroupDefinition implements PatternGroupDefinition {
    private String lotteryConfigId;
    
    @JsonDeserialize(contentAs = DefaultPatternGroup.class)
    private Map<PatternType, PatternGroup> groups;
    
    private int totalAnalyzedDraws;
    private LocalDateTime lastAnalysisDate;
    
    @Data
    public static class DefaultPatternGroup implements PatternGroup {
        private List<String> patterns;
        private List<Integer> frequency;
        private double efficiencyMultiplier;
        private LocalDateTime lastUpdated;
    }
}