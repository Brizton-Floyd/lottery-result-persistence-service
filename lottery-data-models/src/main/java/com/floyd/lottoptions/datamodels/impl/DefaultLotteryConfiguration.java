package com.floyd.lottoptions.datamodels.impl;

import com.floyd.lottoptions.datamodels.LotteryConfiguration;
import lombok.Data;

import java.util.Map;

@Data
public class DefaultLotteryConfiguration implements LotteryConfiguration {
    private String id;
    private String name;
    private NumberRange numberRange;
    private int drawSize;
    private int patternLength;
    private Map<String, PrizeDetails> prizeStructure;
    
    @Data
    public static class DefaultNumberRange implements NumberRange {
        private int min;
        private int max;
    }
    
    @Data
    public static class DefaultPrizeDetails implements PrizeDetails {
        private String tierName;
        private int matchCount;
        private String description;
        private boolean active;
    }
}