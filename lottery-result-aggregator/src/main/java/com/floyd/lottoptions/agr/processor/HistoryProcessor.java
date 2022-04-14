package com.floyd.lottoptions.agr.processor;

import com.floyd.lottoptions.agr.config.LotteryUrlConfig;

import java.util.List;

public interface HistoryProcessor {
    void getHistoricalData(String name, List<LotteryUrlConfig.GameInfo> gameInfo) throws Exception;
}
