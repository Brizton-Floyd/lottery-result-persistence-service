package com.floyd.lottoptions.agr.polling;

import com.floyd.lottoptions.agr.config.LotteryUrlConfig;
import com.floyd.lottoptions.agr.processor.HistoryProcessor;
import com.floyd.lottoptions.agr.processor.LotteryHistoryProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LotteryResultPollingService implements PollingService {

    protected static final Logger log = LoggerFactory.getLogger(LotteryResultPollingService.class);
    private LotteryUrlConfig lotteryUrlConfig;
    private final LotteryHistoryProcessorFactory processorFactory;

    public LotteryResultPollingService(LotteryUrlConfig lotteryUrlConfig, LotteryHistoryProcessorFactory lotteryHistoryProcessorFactory) {
        this.lotteryUrlConfig = lotteryUrlConfig;
        this.processorFactory = lotteryHistoryProcessorFactory;
    }

    @Override
    public void pollForUpdatesToDrawResults() throws Exception {
        log.info("Updating state lotteries");
        // Grab all files ending with ser extension
        List<LotteryUrlConfig.GameUrlsForState> allStateGames = lotteryUrlConfig.getGameUrls();
        for (LotteryUrlConfig.GameUrlsForState gameInfo : allStateGames) {
            final String lotteryStateName = gameInfo.getName().toUpperCase();
            final HistoryProcessor historyProcessor =
                    processorFactory.getLottoHistoryProcessor(lotteryStateName);

            if (historyProcessor != null) {
                historyProcessor.getHistoricalData(gameInfo.getName(), gameInfo.getGameInfo());
            }
        }
    }

}
