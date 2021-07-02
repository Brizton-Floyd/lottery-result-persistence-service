package com.floyd.lottoptions.agr.service.impl;

import com.floyd.lottoptions.agr.config.LotteryUrlConfig;
import com.floyd.lottoptions.agr.service.DataFetcher;
import com.floyd.lottoptions.agr.service.PollingService;
import com.floyd.lottoptions.agr.service.PollingServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LotteryResultPollingService implements PollingService {

    protected static final Logger log = LoggerFactory.getLogger(LotteryResultPollingService.class);
    private LotteryUrlConfig lotteryUrlConfig;
    private PollingServiceManager pollingServiceManger;

    public LotteryResultPollingService(LotteryUrlConfig lotteryUrlConfig, PollingServiceManager pollingServiceManager) {
        this.lotteryUrlConfig = lotteryUrlConfig;
        this.pollingServiceManger = pollingServiceManager;
    }

    @Override
    public void pollForUpdatesToDrawResults() throws Exception {
        log.info("Updating state lotteries");
        // Grab all files ending with ser extension
        List<LotteryUrlConfig.GameUrlsForState> allStateGames = lotteryUrlConfig.getGameUrls();
        for (LotteryUrlConfig.GameUrlsForState gameInfo : allStateGames) {

            DataFetcher dataFetcher =
                    pollingServiceManger.getPollingService(PollingServiceManager.State.valueOf(gameInfo.getName().toUpperCase()));

            if (dataFetcher != null) {
                dataFetcher.getData(gameInfo.getName(), gameInfo.getGameInfo());
            }
        }
    }

}
