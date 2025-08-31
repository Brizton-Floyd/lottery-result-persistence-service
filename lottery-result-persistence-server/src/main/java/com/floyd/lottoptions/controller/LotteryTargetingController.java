package com.floyd.lottoptions.controller;

import com.floyd.lottoptions.datamodels.LotteryConfiguration;
import com.floyd.lottoptions.datamodels.PatternGroupDefinition;
import com.floyd.lottoptions.datamodels.UserRequest;
import com.floyd.lottoptions.datamodels.ProvenCombination;
import com.floyd.lottoptions.datamodels.HotNumber;
import com.floyd.lottoptions.datamodels.LotteryTicket;
import com.floyd.lottoptions.datamodels.GeneratedTicketSet;
import com.floyd.lottoptions.datamodels.LotteryDraw;
import com.floyd.lottoptions.datamodels.NumberFrequency;
import com.floyd.lottoptions.datamodels.impl.DefaultPatternGroupDefinition;
import com.floyd.lottoptions.persistence.LotteryDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    // Proven Combination Endpoints
    @PostMapping("/combinations")
    public ResponseEntity<ProvenCombination> createProvenCombination(@RequestBody ProvenCombination combination) {
        log.debug("Creating proven combination for lottery: {}", combination.getLotteryConfigId());
        ProvenCombination savedCombination = lotteryDataRepository.saveProvenCombination(combination);
        return ResponseEntity.ok(savedCombination);
    }

    @GetMapping("/combinations/{lotteryConfigId}")
    public ResponseEntity<List<ProvenCombination>> getProvenCombinations(@PathVariable String lotteryConfigId) {
        log.debug("Fetching proven combinations for lottery config: {}", lotteryConfigId);
        List<ProvenCombination> combinations = lotteryDataRepository.findProvenCombinationsByLotteryConfig(lotteryConfigId);
        return ResponseEntity.ok(combinations);
    }

    @GetMapping("/combinations/{lotteryConfigId}/high-quality")
    public ResponseEntity<List<ProvenCombination>> getHighQualityCombinations(
            @PathVariable String lotteryConfigId,
            @RequestParam(defaultValue = "70.0") BigDecimal minScore) {
        log.debug("Fetching high quality combinations for lottery config: {} with min score: {}", lotteryConfigId, minScore);
        List<ProvenCombination> combinations = lotteryDataRepository.findHighQualityCombinations(lotteryConfigId, minScore);
        return ResponseEntity.ok(combinations);
    }

    @GetMapping("/combinations/{lotteryConfigId}/winners")
    public ResponseEntity<List<ProvenCombination>> getWinningCombinations(@PathVariable String lotteryConfigId) {
        log.debug("Fetching winning combinations for lottery config: {}", lotteryConfigId);
        List<ProvenCombination> combinations = lotteryDataRepository.findWinningCombinations(lotteryConfigId);
        return ResponseEntity.ok(combinations);
    }
    
    // Hot Number Endpoints
    @PostMapping("/hot-numbers")
    public ResponseEntity<HotNumber> createHotNumber(@RequestBody HotNumber hotNumber) {
        log.debug("Creating hot number with pattern: {}", hotNumber.getNumberPattern());
        HotNumber savedHotNumber = lotteryDataRepository.saveHotNumber(hotNumber);
        return ResponseEntity.ok(savedHotNumber);
    }
    
    @PostMapping("/hot-numbers/batch")
    public ResponseEntity<List<HotNumber>> createHotNumbers(@RequestBody List<HotNumber> hotNumbers) {
        log.debug("Creating {} hot numbers", hotNumbers.size());
        List<HotNumber> savedHotNumbers = hotNumbers.stream()
                .map(lotteryDataRepository::saveHotNumber)
                .collect(Collectors.toList());
        return ResponseEntity.ok(savedHotNumbers);
    }
    
    @GetMapping("/hot-numbers/{lotteryConfigId}")
    public ResponseEntity<List<HotNumber>> getHotNumbers(@PathVariable String lotteryConfigId) {
        log.debug("Fetching hot numbers for lottery config: {}", lotteryConfigId);
        List<HotNumber> hotNumbers = lotteryDataRepository.findHotNumbersByLotteryConfig(lotteryConfigId);
        return ResponseEntity.ok(hotNumbers);
    }
    
    @GetMapping("/hot-numbers/pattern-group/{patternGroupId}")
    public ResponseEntity<List<HotNumber>> getHotNumbersByPatternGroup(@PathVariable Long patternGroupId) {
        log.debug("Fetching hot numbers for pattern group: {}", patternGroupId);
        List<HotNumber> hotNumbers = lotteryDataRepository.findHotNumbersByPatternGroup(patternGroupId);
        return ResponseEntity.ok(hotNumbers);
    }
    
    @GetMapping("/hot-numbers/{lotteryConfigId}/pattern-type/{patternType}")
    public ResponseEntity<List<HotNumber>> getHotNumbersByPatternType(
            @PathVariable String lotteryConfigId, 
            @PathVariable String patternType) {
        log.debug("Fetching hot numbers for lottery config: {} and pattern type: {}", lotteryConfigId, patternType);
        List<HotNumber> hotNumbers = lotteryDataRepository.findHotNumbersByLotteryConfigAndPatternType(lotteryConfigId, patternType);
        return ResponseEntity.ok(hotNumbers);
    }
    
    // Lottery Ticket Endpoints
    @PostMapping("/tickets")
    public ResponseEntity<LotteryTicket> createLotteryTicket(@RequestBody LotteryTicket ticket) {
        log.debug("Creating lottery ticket with ID: {}", ticket.getTicketId());
        LotteryTicket savedTicket = lotteryDataRepository.saveLotteryTicket(ticket);
        return ResponseEntity.ok(savedTicket);
    }
    
    @PostMapping("/tickets/batch")
    public ResponseEntity<List<LotteryTicket>> createLotteryTickets(@RequestBody List<LotteryTicket> tickets) {
        log.debug("Creating {} lottery tickets", tickets.size());
        List<LotteryTicket> savedTickets = tickets.stream()
                .map(lotteryDataRepository::saveLotteryTicket)
                .collect(Collectors.toList());
        return ResponseEntity.ok(savedTickets);
    }
    
    @GetMapping("/tickets/ticket-set/{ticketSetId}")
    public ResponseEntity<List<LotteryTicket>> getLotteryTicketsByTicketSet(@PathVariable Long ticketSetId) {
        log.debug("Fetching lottery tickets for ticket set: {}", ticketSetId);
        List<LotteryTicket> tickets = lotteryDataRepository.findLotteryTicketsByTicketSetId(ticketSetId);
        return ResponseEntity.ok(tickets);
    }
    
    @GetMapping("/tickets/pattern/{pattern}")
    public ResponseEntity<List<LotteryTicket>> getLotteryTicketsByPattern(@PathVariable String pattern) {
        log.debug("Fetching lottery tickets for pattern: {}", pattern);
        List<LotteryTicket> tickets = lotteryDataRepository.findLotteryTicketsByPattern(pattern);
        return ResponseEntity.ok(tickets);
    }
    
    // Generated Ticket Set Endpoints
    @PostMapping("/ticket-sets")
    public ResponseEntity<GeneratedTicketSet> createTicketSet(@RequestBody GeneratedTicketSet ticketSet) {
        log.debug("Creating ticket set for session: {}", ticketSet.getSessionId());
        GeneratedTicketSet savedTicketSet = lotteryDataRepository.saveGeneratedTicketSet(ticketSet);
        return ResponseEntity.ok(savedTicketSet);
    }
    
    @GetMapping("/ticket-sets/session/{sessionId}")
    public ResponseEntity<List<GeneratedTicketSet>> getTicketSetsBySession(@PathVariable String sessionId) {
        log.debug("Fetching ticket sets for session: {}", sessionId);
        List<GeneratedTicketSet> ticketSets = lotteryDataRepository.findGeneratedTicketSetsBySessionId(sessionId);
        return ResponseEntity.ok(ticketSets);
    }
    
    // Lottery Draw Endpoints
    @PostMapping("/draws")
    public ResponseEntity<LotteryDraw> createLotteryDraw(@RequestBody LotteryDraw draw) {
        log.debug("Creating lottery draw for config: {}", draw.getLotteryConfigId());
        LotteryDraw savedDraw = lotteryDataRepository.saveLotteryDraw(draw);
        return ResponseEntity.ok(savedDraw);
    }
    
    @GetMapping("/draws/{configId}")
    public ResponseEntity<List<LotteryDraw>> getLotteryDraws(
            @PathVariable String configId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        log.debug("Fetching lottery draws for config: {} with limit: {} offset: {}", configId, limit, offset);
        List<LotteryDraw> draws = lotteryDataRepository.findLotteryDrawsByConfigId(configId, limit, offset);
        return ResponseEntity.ok(draws);
    }
    
    @GetMapping("/draws/{configId}/latest")
    public ResponseEntity<LotteryDraw> getLatestLotteryDraw(@PathVariable String configId) {
        log.debug("Fetching latest lottery draw for config: {}", configId);
        Optional<LotteryDraw> latestDraw = lotteryDataRepository.findLatestLotteryDraw(configId);
        return latestDraw.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    // Number Frequency Endpoints
    @PostMapping("/frequencies")
    public ResponseEntity<NumberFrequency> createOrUpdateNumberFrequency(@RequestBody NumberFrequency frequency) {
        log.debug("Creating/updating number frequency for config: {}", frequency.getLotteryConfigId());
        NumberFrequency savedFrequency = lotteryDataRepository.saveNumberFrequency(frequency);
        return ResponseEntity.ok(savedFrequency);
    }
    
    @GetMapping("/frequencies/{configId}")
    public ResponseEntity<NumberFrequency> getNumberFrequency(@PathVariable String configId) {
        log.debug("Fetching number frequency for config: {}", configId);
        Optional<NumberFrequency> frequency = lotteryDataRepository.findNumberFrequencyByConfigId(configId);
        return frequency.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}