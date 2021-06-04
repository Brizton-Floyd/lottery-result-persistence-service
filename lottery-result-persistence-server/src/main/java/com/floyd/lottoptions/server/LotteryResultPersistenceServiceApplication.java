package com.floyd.lottoptions.server;

import com.floyd.lottoptions.agr.repository.LotteryStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages =  {"com.floyd"})
@EnableMongoRepositories(basePackageClasses = LotteryStateRepository.class)
public class LotteryResultPersistenceServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(LotteryResultPersistenceServiceApplication.class);

    public static void main(String[] args) {
        log.info("im starting");
        SpringApplication.run(LotteryResultPersistenceServiceApplication.class, args);
    }
}

