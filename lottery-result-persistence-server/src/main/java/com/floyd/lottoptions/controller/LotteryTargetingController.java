package com.floyd.lottoptions.controller;

import com.floyd.lottoptions.datamodels.LotteryConfiguration;
import com.floyd.lottoptions.datamodels.PatternGroupDefinition;
import com.floyd.lottoptions.datamodels.UserRequest;
import com.floyd.lottoptions.datamodels.impl.DefaultPatternGroupDefinition;
import com.floyd.lottoptions.persistence.LotteryDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/lottery-targeting")
@RequiredArgsConstructor
@Slf4j
@Profile("h2")
public class LotteryTargetingController {

    private final LotteryDataRepository lotteryDataRepository;

    @GetMapping("/configurations")
    public ResponseEntity<List<LotteryConfiguration>> getAllLotteryConfigurations() {
        log.debug("Fetching all lottery configurations");
        List<LotteryConfiguration> configurations = lotteryDataRepository.findAllLotteryConfigurations();
        return ResponseEntity.ok(configurations);
    }

    @GetMapping("/configurations/{configId}")
    public ResponseEntity<LotteryConfiguration> getLotteryConfiguration(@PathVariable String configId) {
        log.debug("Fetching lottery configuration for ID: {}", configId);
        Optional<LotteryConfiguration> config = lotteryDataRepository.findLotteryConfiguration(configId);
        return config.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/patterns/{configId}")
    public ResponseEntity<PatternGroupDefinition> getPatternGroups(@PathVariable String configId) {
        log.debug("Fetching pattern groups for lottery config: {}", configId);
        Optional<PatternGroupDefinition> patterns = lotteryDataRepository.findPatternGroupDefinition(configId);
        return patterns.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/patterns/{configId}")
    public ResponseEntity<String> storePatternGroups(@PathVariable String configId, @RequestBody DefaultPatternGroupDefinition patterns) {
        log.debug("Storing pattern groups for lottery config: {}", configId);
        lotteryDataRepository.savePatternGroupDefinition(patterns);
        return ResponseEntity.ok("Pattern groups stored successfully for config: " + configId);
    }

    @PostMapping("/configurations")
    public ResponseEntity<LotteryConfiguration> createLotteryConfiguration(@RequestBody LotteryConfiguration config) {
        log.debug("Creating new lottery configuration: {}", config.getName());
        LotteryConfiguration savedConfig = lotteryDataRepository.saveLotteryConfiguration(config);
        return ResponseEntity.ok(savedConfig);
    }

    @PostMapping("/sessions")
    public ResponseEntity<String> createUserSession(@RequestBody UserRequest userRequest) {
        log.debug("Creating user session for lottery: {} targeting tier: {}", 
                 userRequest.getLotteryConfigId(), userRequest.getTargetTier());
        String sessionId = lotteryDataRepository.saveUserSession(userRequest);
        return ResponseEntity.ok(sessionId);
    }

    @GetMapping("/sessions/by-tier/{targetTier}")
    public ResponseEntity<List<UserRequest>> getSessionsByTier(@PathVariable String targetTier) {
        log.debug("Fetching sessions for target tier: {}", targetTier);
        List<UserRequest> sessions = lotteryDataRepository.findUserSessionsByTier(targetTier);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/recent")
    public ResponseEntity<List<UserRequest>> getRecentSessions(@RequestParam(defaultValue = "7") int daysBack) {
        log.debug("Fetching sessions from last {} days", daysBack);
        List<UserRequest> sessions = lotteryDataRepository.findRecentUserSessions(daysBack);
        return ResponseEntity.ok(sessions);
    }
}