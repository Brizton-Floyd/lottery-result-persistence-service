package com.floyd.lottoptions.agr.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Component
public class ApplicationConfig {

    @Bean
    public WebClient getWebClientBuilder(){
        return   WebClient.builder().exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024))
                .build())
                .build();
    }

    @Bean
    public RateLimiter getRateLimiter() {
        // enables 20 requests every 30 seconds
        return RateLimiter.of("my-rate-limiter",
                RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(5))
                        .limitForPeriod(3)
                        .timeoutDuration(Duration.ofMinutes(1)) // max wait time for a request, if reached then error
                        .build());
    }
}
