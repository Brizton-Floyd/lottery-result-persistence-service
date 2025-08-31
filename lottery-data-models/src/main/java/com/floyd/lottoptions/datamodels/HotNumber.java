package com.floyd.lottoptions.datamodels;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HotNumber {
    private Long id;
    private String numberPattern;
    private Integer frequencyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long patternGroupId;
}