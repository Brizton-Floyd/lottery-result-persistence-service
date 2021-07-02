package com.floyd.lottoptions.agr.task;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

import javax.annotation.PostConstruct;

import com.floyd.lottoptions.agr.service.impl.LotteryResultPollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

@Component
public class DrawResultAggregator {

    private static final Logger log = LoggerFactory.getLogger(DrawResultAggregator.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private static final String drawCronExpression = "0 0 */2 * * ?";

    private final LotteryResultPollingService lotteryResultPollingService;

    public DrawResultAggregator(LotteryResultPollingService lotteryResultPollingService) {
        this.lotteryResultPollingService = lotteryResultPollingService;
    }

//    @Scheduled(cron = "0 30 03 01 */3 *")
//    public void reportCurrentTime() {
//        log.info("Aggregating US Lottery Results {}", dateFormat.format(new Date()));
//        try {
//            lotteryResultPollingService.pollForUpdatesToStateGames();
//        } catch (Exception e) {
//            log.debug(e.getMessage());
//        }
//    }

    @Scheduled(cron = drawCronExpression)
    public void performDrawResultUpdates() {
        log.info("Aggregating US Lottery Results Draw Results{}", dateFormat.format(new Date()));
        try {
            lotteryResultPollingService.pollForUpdatesToDrawResults();
        } catch (Exception e) {
            log.debug(e.getMessage());
        } 
        finally {
            logNextRunTime();
        }
    }

    @PostConstruct
    public void logNextRunTime() {
        CronExpression cronTrigger = CronExpression.parse(drawCronExpression);

        LocalDateTime next = cronTrigger.next(LocalDateTime.now());

        log.info("Next Execution Time: " + next);
    }
}
