package com.floyd.lottoptions.datamodels;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class LotteryTicket {
    private String ticketId;
    private Long ticketSetId;
    private List<Integer> numbers;
    private String patternUsed;
    private BigDecimal confidenceScore;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}