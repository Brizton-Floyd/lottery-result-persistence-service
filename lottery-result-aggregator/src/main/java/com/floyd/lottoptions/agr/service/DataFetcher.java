package com.floyd.lottoptions.agr.service;

import com.floyd.lottoptions.agr.config.LotteryUrlConfig;

import java.util.List;

public interface DataFetcher {
    void getData(String name, List<LotteryUrlConfig.GameInfo> gameInfo) throws Exception;
}
