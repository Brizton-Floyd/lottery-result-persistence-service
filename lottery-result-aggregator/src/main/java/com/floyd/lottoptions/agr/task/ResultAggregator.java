package com.floyd.lottoptions.agr.task;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.floyd.lottoptions.agr.service.impl.LotteryResultPollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ResultAggregator {

    private static final Logger log = LoggerFactory.getLogger(ResultAggregator.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private final LotteryResultPollingService lotteryResultPollingService;

    public ResultAggregator(LotteryResultPollingService lotteryResultPollingService) {
        this.lotteryResultPollingService = lotteryResultPollingService;
    }

    @Scheduled(cron = "0 30 03 01 */3 *")
    public void reportCurrentTime() {
        log.info("Aggregating US Lottery Results {}", dateFormat.format(new Date()));
        try {
            lotteryResultPollingService.pollForUpdatesToStateGames();
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void performDrawResultUpdates() {
        log.info("Aggregating US Lottery Results Draw Results{}", dateFormat.format(new Date()));
        try {
            lotteryResultPollingService.pollForUpdatesToDrawResults();
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }
}
