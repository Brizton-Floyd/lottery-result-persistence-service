package com.floyd.lottoptions.datamodels;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ProvenCombination {
    private Long id;
    private String lotteryConfigId;
    private List<Integer> combinationNumbers;
    private Integer frequencyCount;
    private LocalDate lastDrawnDate;
    private Integer winCount;
    private Map<String, Integer> tierPerformance; // e.g., {"tier_1": 2, "tier_2": 5}
    private BigDecimal qualityScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}