package com.floyd.lottoptions.controller;

import com.floyd.lottoptions.controller.response.LotteryTargetingResponse;
import com.floyd.lottoptions.datamodels.LotteryConfiguration;
import com.floyd.lottoptions.datamodels.PatternGroupDefinition;
import com.floyd.lottoptions.datamodels.UserRequest;
import com.floyd.lottoptions.persistence.LotteryDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/lottery-targeting/summary")
@RequiredArgsConstructor
@Slf4j
@Profile("h2")
public class LotteryTargetingSummaryController {

    private final LotteryDataRepository lotteryDataRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSystemSummary() {
        log.debug("Fetching lottery targeting system summary");
        
        Map<String, Object> summary = new HashMap<>();
        
        // Get configuration summary
        List<LotteryConfiguration> configs = lotteryDataRepository.findAllLotteryConfigurations();
        List<LotteryTargetingResponse.ConfigurationSummary> configSummaries = configs.stream()
            .map(this::mapToConfigurationSummary)
            .collect(Collectors.toList());
        
        summary.put("totalConfigurations", configs.size());
        summary.put("configurations", configSummaries);
        
        // Get recent sessions summary
        List<UserRequest> recentSessions = lotteryDataRepository.findRecentUserSessions(30);
        List<LotteryTargetingResponse.SessionSummary> sessionSummaries = recentSessions.stream()
            .map(this::mapToSessionSummary)
            .collect(Collectors.toList());
            
        summary.put("recentSessionsCount", recentSessions.size());
        summary.put("recentSessions", sessionSummaries);
        
        // Get pattern analysis summary
        Map<String, Object> patternStats = new HashMap<>();
        for (LotteryConfiguration config : configs) {
            lotteryDataRepository.findPatternGroupDefinition(config.getId())
                .ifPresent(patterns -> {
                    Map<String, Object> configPatterns = new HashMap<>();
                    configPatterns.put("totalAnalyzedDraws", patterns.getTotalAnalyzedDraws());
                    configPatterns.put("lastAnalysis", patterns.getLastAnalysisDate());
                    configPatterns.put("patternGroupCount", patterns.getGroups().size());
                    patternStats.put(config.getName(), configPatterns);
                });
        }
        summary.put("patternAnalysis", patternStats);
        
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/available-games")
    public ResponseEntity<List<Map<String, Object>>> getAvailableGames() {
        log.debug("Fetching available lottery games for targeting");
        
        List<LotteryConfiguration> configs = lotteryDataRepository.findAllLotteryConfigurations();
        List<Map<String, Object>> games = configs.stream()
            .map(config -> {
                Map<String, Object> game = new HashMap<>();
                game.put("id", config.getId());
                game.put("name", config.getName());
                game.put("numberRange", config.getNumberRange().getMin() + "-" + config.getNumberRange().getMax());
                game.put("drawSize", config.getDrawSize());
                game.put("availableTiers", config.getPrizeStructure().keySet());
                return game;
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(games);
    }

    private LotteryTargetingResponse.ConfigurationSummary mapToConfigurationSummary(LotteryConfiguration config) {
        LotteryTargetingResponse.ConfigurationSummary summary = new LotteryTargetingResponse.ConfigurationSummary();
        summary.setId(config.getId());
        summary.setName(config.getName());
        summary.setNumberRange(config.getNumberRange().getMin() + "-" + config.getNumberRange().getMax());
        summary.setDrawSize(config.getDrawSize());
        summary.setAvailableTiers(config.getPrizeStructure().keySet().stream().collect(Collectors.toList()));
        return summary;
    }

    private LotteryTargetingResponse.SessionSummary mapToSessionSummary(UserRequest session) {
        LotteryTargetingResponse.SessionSummary summary = new LotteryTargetingResponse.SessionSummary();
        summary.setSessionId(session.getSessionId());
        summary.setLotteryGame(session.getLotteryConfigId());
        summary.setTargetTier(session.getTargetTier());
        summary.setTicketsRequested(session.getNumberOfTickets());
        summary.setStrategy(session.getGenerationStrategy().name());
        summary.setCreatedAt(session.getTimestamp());
        return summary;
    }
}