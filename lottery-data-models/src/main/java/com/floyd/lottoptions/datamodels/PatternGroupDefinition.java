package com.floyd.lottoptions.datamodels;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PatternGroupDefinition {
    String getLotteryConfigId();
    Map<PatternType, PatternGroup> getGroups();
    int getTotalAnalyzedDraws();
    LocalDateTime getLastAnalysisDate();
    
    enum PatternType {
        HOT, WARM, COLD
    }
    
    interface PatternGroup {
        List<String> getPatterns();
        List<Integer> getFrequency();
        double getEfficiencyMultiplier();
        LocalDateTime getLastUpdated();
    }
}