package com.floyd.lottoptions.datamodels;

import java.util.Map;

public interface LotteryConfiguration {
    String getId();
    String getName();
    NumberRange getNumberRange();
    int getDrawSize();
    int getPatternLength();
    Map<String, PrizeDetails> getPrizeStructure();
    
    interface NumberRange {
        int getMin();
        int getMax();
    }
    
    interface PrizeDetails {
        String getTierName();
        int getMatchCount();
        String getDescription();
        boolean isActive();
    }
}