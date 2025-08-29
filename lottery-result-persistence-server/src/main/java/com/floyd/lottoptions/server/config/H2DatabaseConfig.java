package com.floyd.lottoptions.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@Configuration
@Profile("h2")
@EnableJpaRepositories(basePackages = "com.floyd.lottoptions.persistence.repository")
@EntityScan(basePackages = "com.floyd.lottoptions.persistence.entity")
public class H2DatabaseConfig {
}