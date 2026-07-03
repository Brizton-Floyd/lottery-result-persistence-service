package com.floyd.lottoptions.agr.polling;

import com.floyd.lottoptions.agr.config.LotteryUrlConfig;
import com.floyd.lottoptions.agr.processor.HistoryProcessor;
import com.floyd.lottoptions.agr.processor.LotteryHistoryProcessorFactory;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class LotteryResultPollingService implements PollingService {

    protected static final Logger log = LoggerFactory.getLogger(LotteryResultPollingService.class);

    /** Resilience4j instance name; configured under {@code resilience4j.*.instances.lotteryFetch}. */
    private static final String RESILIENCE_INSTANCE = "lotteryFetch";

    private final LotteryUrlConfig lotteryUrlConfig;
    private final LotteryHistoryProcessorFactory processorFactory;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final int poolSize;

    public LotteryResultPollingService(LotteryUrlConfig lotteryUrlConfig,
                                       LotteryHistoryProcessorFactory lotteryHistoryProcessorFactory,
                                       RetryRegistry retryRegistry,
                                       RateLimiterRegistry rateLimiterRegistry,
                                       ApplicationEventPublisher eventPublisher,
                                       @Value("${lottery.polling.pool-size:4}") int poolSize) {
        this.lotteryUrlConfig = lotteryUrlConfig;
        this.processorFactory = lotteryHistoryProcessorFactory;
        this.retryRegistry = retryRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.eventPublisher = eventPublisher;
        this.poolSize = poolSize;
    }

    @Override
    public void pollForUpdatesToDrawResults() throws Exception {
        log.info("Updating state lotteries");
        final Retry retry = retryRegistry.retry(RESILIENCE_INSTANCE);
        final RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(RESILIENCE_INSTANCE);

        final List<Callable<Boolean>> tasks = buildTasks(retry, rateLimiter);
        if (tasks.isEmpty()) {
            log.warn("No supported state/game combinations to poll.");
            eventPublisher.publishEvent(new PollCompletedEvent(0, 0));
            return;
        }

        int succeeded = 0;
        int failed = 0;
        final ExecutorService pool = Executors.newFixedThreadPool(Math.min(poolSize, tasks.size()));
        try {
            for (Future<Boolean> result : pool.invokeAll(tasks)) {
                try {
                    if (Boolean.TRUE.equals(result.get())) {
                        succeeded++;
                    } else {
                        failed++;
                    }
                } catch (ExecutionException e) {
                    // Individual game failures are already logged in fetchOneGame; count and continue.
                    failed++;
                }
            }
        } finally {
            pool.shutdown();
        }

        log.info("Poll complete: {} game(s) succeeded, {} game(s) failed", succeeded, failed);
        eventPublisher.publishEvent(new PollCompletedEvent(succeeded, failed));
    }

    private List<Callable<Boolean>> buildTasks(Retry retry, RateLimiter rateLimiter) {
        final List<Callable<Boolean>> tasks = new ArrayList<>();
        for (LotteryUrlConfig.GameUrlsForState stateGames : lotteryUrlConfig.getGameUrls()) {
            final String stateName = stateGames.getName();
            final String stateKey = stateName.toUpperCase();
            final List<LotteryUrlConfig.GameInfo> games = stateGames.getGameInfo();

            if (processorFactory.getLottoHistoryProcessor(stateKey) == null) {
                log.warn("No processor for state '{}'; skipping {} game(s).",
                        stateName, games == null ? 0 : games.size());
                continue;
            }
            if (games == null) {
                continue;
            }
            for (LotteryUrlConfig.GameInfo game : games) {
                tasks.add(() -> fetchOneGame(stateKey, stateName, game, retry, rateLimiter));
            }
        }
        return tasks;
    }

    /**
     * Fetch and persist a single game with a fresh processor (so no reader state is shared
     * across threads), wrapped in a rate limiter (per attempt) and retry. A failure here is
     * isolated: it is logged and reported as {@code false} without aborting the batch.
     */
    private boolean fetchOneGame(String stateKey, String stateName, LotteryUrlConfig.GameInfo game,
                                 Retry retry, RateLimiter rateLimiter) {
        final HistoryProcessor processor = processorFactory.getLottoHistoryProcessor(stateKey);
        if (processor == null) {
            return false;
        }
        final CheckedRunnable resilient = Retry.decorateCheckedRunnable(retry,
                RateLimiter.decorateCheckedRunnable(rateLimiter,
                        () -> processor.processGame(stateName, game)));
        try {
            resilient.run();
            return true;
        } catch (Throwable t) {
            log.error("Failed to update game '{}' for state '{}' after retries",
                    game.getName(), stateName, t);
            return false;
        }
    }
}
