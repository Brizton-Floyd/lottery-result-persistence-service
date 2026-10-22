package com.floyd.lottoptions.agr.polling;

import com.floyd.lottoptions.agr.config.LotteryUrlConfig;
import com.floyd.lottoptions.agr.processor.GameProcessingResult;
import com.floyd.lottoptions.agr.processor.HistoryProcessor;
import com.floyd.lottoptions.agr.processor.LotteryHistoryProcessorFactory;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LotteryResultPollingServiceTest {

    private static LotteryUrlConfig.GameInfo game(String name) {
        LotteryUrlConfig.GameInfo info = new LotteryUrlConfig.GameInfo();
        info.setName(name);
        info.setUrl("file:///unused");
        return info;
    }

    @Test
    void poll_reportsGamesWithRejectedRowsSeparately_ratherThanAsSucceeded() throws Exception {
        LotteryUrlConfig.GameUrlsForState texas = new LotteryUrlConfig.GameUrlsForState();
        texas.setName("Texas");
        texas.setGameInfo(List.of(game("Clean"), game("Partial"), game("Broken")));
        LotteryUrlConfig config = new LotteryUrlConfig();
        config.setStates(List.of(texas));

        Map<String, GameProcessingResult> outcomes = Map.of(
                "Clean", new GameProcessingResult(10, 0),
                "Partial", new GameProcessingResult(10, 3));
        HistoryProcessor processor = new HistoryProcessor() {
            @Override
            public void getHistoricalData(String name, List<LotteryUrlConfig.GameInfo> gameInfo) {
                throw new UnsupportedOperationException();
            }

            @Override
            public GameProcessingResult processGame(String stateName, LotteryUrlConfig.GameInfo info) {
                GameProcessingResult result = outcomes.get(info.getName());
                if (result == null) {
                    throw new IllegalStateException("No parseable draws");
                }
                return result;
            }
        };
        LotteryHistoryProcessorFactory factory = new LotteryHistoryProcessorFactory(null, "unused") {
            @Override
            public HistoryProcessor getLottoHistoryProcessor(String state) {
                return processor;
            }
        };

        List<Object> events = new ArrayList<>();
        RetryRegistry retries = RetryRegistry.of(RetryConfig.custom().maxAttempts(1).build());
        new LotteryResultPollingService(config, factory, retries, RateLimiterRegistry.ofDefaults(), events::add, 2)
                .pollForUpdatesToDrawResults();

        assertThat(events).containsExactly(new PollCompletedEvent(1, 1, 1));
    }
}
