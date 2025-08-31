package com.floyd.lottoptions.persistence;

import com.floyd.lottoptions.datamodels.LotteryConfiguration;
import com.floyd.lottoptions.datamodels.PatternGroupDefinition;
import com.floyd.lottoptions.datamodels.UserRequest;
import com.floyd.lottoptions.datamodels.GeneratedTicketSet;
import com.floyd.lottoptions.datamodels.ProvenCombination;
import com.floyd.lottoptions.datamodels.HotNumber;
import com.floyd.lottoptions.datamodels.LotteryTicket;
import com.floyd.lottoptions.datamodels.LotteryDraw;
import com.floyd.lottoptions.datamodels.NumberFrequency;
import com.floyd.lottoptions.datamodels.impl.DefaultLotteryConfiguration;
import com.floyd.lottoptions.datamodels.impl.DefaultPatternGroupDefinition;
import com.floyd.lottoptions.persistence.entity.LotteryConfigurationEntity;
import com.floyd.lottoptions.persistence.entity.PatternGroupEntity;
import com.floyd.lottoptions.persistence.entity.UserSessionEntity;
import com.floyd.lottoptions.persistence.entity.ProvenCombinationEntity;
import com.floyd.lottoptions.persistence.entity.HotNumberEntity;
import com.floyd.lottoptions.persistence.entity.LotteryTicketEntity;
import com.floyd.lottoptions.persistence.entity.GeneratedTicketSetEntity;
import com.floyd.lottoptions.persistence.entity.LotteryDrawEntity;
import com.floyd.lottoptions.persistence.entity.NumberFrequencyEntity;
import com.floyd.lottoptions.persistence.repository.LotteryConfigurationRepository;
import com.floyd.lottoptions.persistence.repository.PatternGroupRepository;
import com.floyd.lottoptions.persistence.repository.UserSessionRepository;
import com.floyd.lottoptions.persistence.repository.ProvenCombinationRepository;
import com.floyd.lottoptions.persistence.repository.HotNumberRepository;
import com.floyd.lottoptions.persistence.repository.LotteryTicketRepository;
import com.floyd.lottoptions.persistence.repository.GeneratedTicketSetRepository;
import com.floyd.lottoptions.persistence.repository.LotteryDrawRepository;
import com.floyd.lottoptions.persistence.repository.NumberFrequencyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
@Profile("h2")
public class LotteryDataRepository {
    
    private final LotteryConfigurationRepository lotteryConfigRepository;
    private final PatternGroupRepository patternGroupRepository;
    private final UserSessionRepository userSessionRepository;
    private final ProvenCombinationRepository provenCombinationRepository;
    private final HotNumberRepository hotNumberRepository;
    private final LotteryTicketRepository lotteryTicketRepository;
    private final GeneratedTicketSetRepository generatedTicketSetRepository;
    private final LotteryDrawRepository lotteryDrawRepository;
    private final NumberFrequencyRepository numberFrequencyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public Optional<LotteryConfiguration> findLotteryConfiguration(String configId) {
        return lotteryConfigRepository.findById(configId)
                .map(this::convertToLotteryConfiguration);
    }
    
    public List<LotteryConfiguration> findAllLotteryConfigurations() {
        return lotteryConfigRepository.findAll().stream()
                .map(this::convertToLotteryConfiguration)
                .collect(Collectors.toList());
    }
    
    public Optional<PatternGroupDefinition> findPatternGroupDefinition(String lotteryConfigId) {
        List<PatternGroupEntity> patternGroups = patternGroupRepository.findByLotteryConfigurationId(lotteryConfigId);
        if (patternGroups.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(convertToPatternGroupDefinition(patternGroups));
    }
    
    public LotteryConfiguration saveLotteryConfiguration(LotteryConfiguration config) {
        LotteryConfigurationEntity entity = convertToEntity(config);
        LotteryConfigurationEntity savedEntity = lotteryConfigRepository.save(entity);
        return convertToLotteryConfiguration(savedEntity);
    }
    
    public PatternGroupDefinition savePatternGroupDefinition(PatternGroupDefinition patterns) {
        LotteryConfigurationEntity lotteryConfig = lotteryConfigRepository.findById(patterns.getLotteryConfigId())
                .orElseThrow(() -> new RuntimeException("Lottery configuration not found: " + patterns.getLotteryConfigId()));
        
        patternGroupRepository.deleteByLotteryConfigurationId(patterns.getLotteryConfigId());
        
        for (Map.Entry<PatternGroupDefinition.PatternType, PatternGroupDefinition.PatternGroup> entry : patterns.getGroups().entrySet()) {
            PatternGroupEntity entity = convertToPatternGroupEntity(patterns, entry.getKey(), entry.getValue(), lotteryConfig);
            patternGroupRepository.save(entity);
        }
        
        return patterns;
    }
    
    public String saveUserSession(UserRequest userRequest) {
        UserSessionEntity entity = convertToUserSessionEntity(userRequest);
        UserSessionEntity savedEntity = userSessionRepository.save(entity);
        return savedEntity.getSessionId();
    }
    
    public List<UserRequest> findUserSessionsByTier(String targetTier) {
        return userSessionRepository.findByTargetTier(targetTier).stream()
                .map(this::convertToUserRequest)
                .collect(Collectors.toList());
    }
    
    public List<UserRequest> findRecentUserSessions(int daysBack) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysBack);
        return userSessionRepository.findRecentSessions(cutoffDate).stream()
                .map(this::convertToUserRequest)
                .collect(Collectors.toList());
    }
    
    private LotteryConfiguration convertToLotteryConfiguration(LotteryConfigurationEntity entity) {
        DefaultLotteryConfiguration config = new DefaultLotteryConfiguration();
        config.setId(entity.getId());
        config.setName(entity.getName());
        config.setDrawSize(entity.getDrawSize());
        config.setPatternLength(entity.getPatternLength());
        
        DefaultLotteryConfiguration.DefaultNumberRange range = new DefaultLotteryConfiguration.DefaultNumberRange();
        range.setMin(entity.getMinNumber());
        range.setMax(entity.getMaxNumber());
        config.setNumberRange(range);
        
        if (entity.getPrizeStructures() != null) {
            Map<String, LotteryConfiguration.PrizeDetails> prizeMap = entity.getPrizeStructures().stream()
                    .collect(Collectors.toMap(
                            prize -> prize.getTierName(),
                            prize -> {
                                DefaultLotteryConfiguration.DefaultPrizeDetails details = new DefaultLotteryConfiguration.DefaultPrizeDetails();
                                details.setTierName(prize.getTierName());
                                details.setMatchCount(prize.getMatchCount());
                                details.setDescription(prize.getDescription());
                                details.setActive(prize.getActive());
                                return details;
                            }
                    ));
            config.setPrizeStructure(prizeMap);
        }
        
        return config;
    }
    
    private PatternGroupDefinition convertToPatternGroupDefinition(List<PatternGroupEntity> entities) {
        if (entities.isEmpty()) {
            return null;
        }
        
        DefaultPatternGroupDefinition definition = new DefaultPatternGroupDefinition();
        definition.setLotteryConfigId(entities.get(0).getLotteryConfiguration().getId());
        definition.setTotalAnalyzedDraws(entities.get(0).getTotalAnalyzedDraws());
        definition.setLastAnalysisDate(entities.get(0).getLastAnalysisDate());
        
        Map<PatternGroupDefinition.PatternType, PatternGroupDefinition.PatternGroup> groupMap = new HashMap<>();
        
        for (PatternGroupEntity entity : entities) {
            DefaultPatternGroupDefinition.DefaultPatternGroup group = new DefaultPatternGroupDefinition.DefaultPatternGroup();
            group.setEfficiencyMultiplier(entity.getEfficiencyMultiplier().doubleValue());
            group.setLastUpdated(entity.getLastUpdated());
            
            if (entity.getHotNumbers() != null) {
                group.setPatterns(entity.getHotNumbers().stream()
                        .map(hn -> hn.getNumberPattern())
                        .collect(Collectors.toList()));
                group.setFrequency(entity.getHotNumbers().stream()
                        .map(hn -> hn.getFrequencyCount())
                        .collect(Collectors.toList()));
            } else {
                group.setPatterns(new ArrayList<>());
                group.setFrequency(new ArrayList<>());
            }
            
            PatternGroupDefinition.PatternType patternType = PatternGroupDefinition.PatternType.valueOf(entity.getPatternType().name());
            groupMap.put(patternType, group);
        }
        
        definition.setGroups(groupMap);
        return definition;
    }
    
    private LotteryConfigurationEntity convertToEntity(LotteryConfiguration config) {
        LotteryConfigurationEntity entity = new LotteryConfigurationEntity();
        entity.setId(config.getId());
        entity.setName(config.getName());
        entity.setDrawSize(config.getDrawSize());
        entity.setPatternLength(config.getPatternLength());
        entity.setMinNumber(config.getNumberRange().getMin());
        entity.setMaxNumber(config.getNumberRange().getMax());
        return entity;
    }
    
    private UserSessionEntity convertToUserSessionEntity(UserRequest userRequest) {
        UserSessionEntity entity = new UserSessionEntity();
        entity.setSessionId(userRequest.getSessionId());
        entity.setTargetTier(userRequest.getTargetTier() != null ? userRequest.getTargetTier() : "tier_1");
        entity.setNumberOfTickets(userRequest.getNumberOfTickets());
        entity.setGenerationStrategy(userRequest.getGenerationStrategy() != null ? 
                                   userRequest.getGenerationStrategy().name() : "PATTERN_BASED");
        entity.setBudget(userRequest.getBudget());
        
        try {
            if (userRequest.getPreferences() != null) {
                entity.setPreferences(objectMapper.writeValueAsString(userRequest.getPreferences()));
            }
        } catch (JsonProcessingException e) {
            log.error("Error serializing preferences", e);
        }
        
        if (userRequest.getLotteryConfigId() == null || userRequest.getLotteryConfigId().trim().isEmpty()) {
            throw new IllegalArgumentException("Lottery configuration ID cannot be null or empty");
        }
        
        LotteryConfigurationEntity lotteryConfig = lotteryConfigRepository.findById(userRequest.getLotteryConfigId())
                .orElseThrow(() -> new RuntimeException("Lottery configuration not found: " + userRequest.getLotteryConfigId()));
        entity.setLotteryConfiguration(lotteryConfig);
        
        return entity;
    }
    
    private UserRequest convertToUserRequest(UserSessionEntity entity) {
        UserRequest userRequest = new UserRequest();
        userRequest.setSessionId(entity.getSessionId());
        userRequest.setTargetTier(entity.getTargetTier());
        userRequest.setNumberOfTickets(entity.getNumberOfTickets());
        userRequest.setGenerationStrategy(UserRequest.GenerationStrategy.valueOf(entity.getGenerationStrategy()));
        userRequest.setBudget(entity.getBudget());
        userRequest.setLotteryConfigId(entity.getLotteryConfiguration().getId());
        userRequest.setTimestamp(entity.getCreatedAt());
        
        try {
            if (entity.getPreferences() != null) {
                Map<String, Object> preferences = objectMapper.readValue(entity.getPreferences(), Map.class);
                userRequest.setPreferences(preferences);
            }
        } catch (JsonProcessingException e) {
            log.error("Error deserializing preferences", e);
        }
        
        return userRequest;
    }
    
    private PatternGroupEntity convertToPatternGroupEntity(PatternGroupDefinition patterns, 
                                                          PatternGroupDefinition.PatternType patternType, 
                                                          PatternGroupDefinition.PatternGroup group, 
                                                          LotteryConfigurationEntity lotteryConfig) {
        PatternGroupEntity entity = new PatternGroupEntity();
        entity.setLotteryConfiguration(lotteryConfig);
        entity.setPatternType(PatternGroupEntity.PatternType.valueOf(patternType.name()));
        entity.setEfficiencyMultiplier(BigDecimal.valueOf(group.getEfficiencyMultiplier()));
        entity.setTotalAnalyzedDraws(patterns.getTotalAnalyzedDraws());
        entity.setLastAnalysisDate(patterns.getLastAnalysisDate());
        entity.setLastUpdated(group.getLastUpdated());
        return entity;
    }
    
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void cleanupOldUserSessions() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        int deletedCount = userSessionRepository.deleteSessionsOlderThan(cutoffDate);
        log.info("Automated cleanup: Deleted {} user sessions older than 90 days", deletedCount);
    }
    
    @Scheduled(cron = "0 30 2 * * ?") // Daily at 2:30 AM
    @Transactional  
    public void cleanupOldPatternGroups() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(180);
        log.info("Automated cleanup: Checking for stale pattern groups older than 180 days");
        
        List<PatternGroupDefinition> allPatterns = patternGroupRepository.findAll().stream()
            .collect(Collectors.groupingBy(pg -> pg.getLotteryConfiguration().getId()))
            .entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty() && 
                    entry.getValue().get(0).getLastAnalysisDate() != null &&
                    entry.getValue().get(0).getLastAnalysisDate().isBefore(cutoffDate))
            .map(entry -> convertToPatternGroupDefinition(entry.getValue()))
            .collect(Collectors.toList());
            
        if (!allPatterns.isEmpty()) {
            log.info("Found {} stale pattern group sets to clean up", allPatterns.size());
            for (PatternGroupDefinition pattern : allPatterns) {
                patternGroupRepository.deleteByLotteryConfigurationId(pattern.getLotteryConfigId());
                log.debug("Deleted stale patterns for lottery config: {}", pattern.getLotteryConfigId());
            }
        }
    }
    
    // Proven Combination Methods
    public ProvenCombination saveProvenCombination(ProvenCombination combination) {
        ProvenCombinationEntity entity = convertToProvenCombinationEntity(combination);
        ProvenCombinationEntity savedEntity = provenCombinationRepository.save(entity);
        return convertToProvenCombination(savedEntity);
    }
    
    public List<ProvenCombination> findProvenCombinationsByLotteryConfig(String lotteryConfigId) {
        return provenCombinationRepository.findByLotteryConfigurationId(lotteryConfigId).stream()
                .map(this::convertToProvenCombination)
                .collect(Collectors.toList());
    }
    
    public List<ProvenCombination> findHighQualityCombinations(String lotteryConfigId, BigDecimal minScore) {
        return provenCombinationRepository.findHighQualityCombinations(lotteryConfigId, minScore).stream()
                .map(this::convertToProvenCombination)
                .collect(Collectors.toList());
    }
    
    public List<ProvenCombination> findWinningCombinations(String lotteryConfigId) {
        return provenCombinationRepository.findWinningCombinations(lotteryConfigId).stream()
                .map(this::convertToProvenCombination)
                .collect(Collectors.toList());
    }
    
    private ProvenCombinationEntity convertToProvenCombinationEntity(ProvenCombination combination) {
        ProvenCombinationEntity entity = new ProvenCombinationEntity();
        entity.setId(combination.getId());
        
        if (combination.getLotteryConfigId() == null || combination.getLotteryConfigId().trim().isEmpty()) {
            throw new IllegalArgumentException("Lottery configuration ID cannot be null or empty for proven combination");
        }
        
        LotteryConfigurationEntity lotteryConfig = lotteryConfigRepository.findById(combination.getLotteryConfigId())
                .orElseThrow(() -> new RuntimeException("Lottery configuration not found: " + combination.getLotteryConfigId()));
        entity.setLotteryConfiguration(lotteryConfig);
        
        // Convert List<Integer> to comma-separated string
        if (combination.getCombinationNumbers() != null && !combination.getCombinationNumbers().isEmpty()) {
            String numbersString = combination.getCombinationNumbers().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            entity.setCombinationNumbers(numbersString);
        }
        
        entity.setFrequencyCount(combination.getFrequencyCount());
        entity.setLastDrawnDate(combination.getLastDrawnDate());
        entity.setWinCount(combination.getWinCount());
        entity.setQualityScore(combination.getQualityScore());
        
        // Convert tier performance map to JSON string
        try {
            if (combination.getTierPerformance() != null) {
                entity.setTierPerformance(objectMapper.writeValueAsString(combination.getTierPerformance()));
            }
        } catch (JsonProcessingException e) {
            log.error("Error serializing tier performance", e);
        }
        
        return entity;
    }
    
    private ProvenCombination convertToProvenCombination(ProvenCombinationEntity entity) {
        ProvenCombination combination = new ProvenCombination();
        combination.setId(entity.getId());
        combination.setLotteryConfigId(entity.getLotteryConfiguration().getId());
        combination.setFrequencyCount(entity.getFrequencyCount());
        combination.setLastDrawnDate(entity.getLastDrawnDate());
        combination.setWinCount(entity.getWinCount());
        combination.setQualityScore(entity.getQualityScore());
        combination.setCreatedAt(entity.getCreatedAt());
        combination.setUpdatedAt(entity.getUpdatedAt());
        
        // Convert comma-separated string to List<Integer>
        if (entity.getCombinationNumbers() != null && !entity.getCombinationNumbers().trim().isEmpty()) {
            List<Integer> numbers = Arrays.stream(entity.getCombinationNumbers().split(","))
                    .map(String::trim)
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
            combination.setCombinationNumbers(numbers);
        }
        
        // Convert JSON string to tier performance map
        try {
            if (entity.getTierPerformance() != null) {
                Map<String, Integer> tierPerformance = objectMapper.readValue(entity.getTierPerformance(), Map.class);
                combination.setTierPerformance(tierPerformance);
            }
        } catch (JsonProcessingException e) {
            log.error("Error deserializing tier performance", e);
        }
        
        return combination;
    }
    
    // Hot Number Methods
    public HotNumber saveHotNumber(HotNumber hotNumber) {
        HotNumberEntity entity = convertToHotNumberEntity(hotNumber);
        HotNumberEntity savedEntity = hotNumberRepository.save(entity);
        return convertToHotNumber(savedEntity);
    }
    
    public List<HotNumber> findHotNumbersByLotteryConfig(String lotteryConfigId) {
        return hotNumberRepository.findByLotteryConfigId(lotteryConfigId).stream()
                .map(this::convertToHotNumber)
                .collect(Collectors.toList());
    }
    
    public List<HotNumber> findHotNumbersByPatternGroup(Long patternGroupId) {
        return hotNumberRepository.findByPatternGroupId(patternGroupId).stream()
                .map(this::convertToHotNumber)
                .collect(Collectors.toList());
    }
    
    public List<HotNumber> findHotNumbersByLotteryConfigAndPatternType(String lotteryConfigId, String patternType) {
        return hotNumberRepository.findByLotteryConfigIdAndPatternType(lotteryConfigId, patternType).stream()
                .map(this::convertToHotNumber)
                .collect(Collectors.toList());
    }
    
    private HotNumberEntity convertToHotNumberEntity(HotNumber hotNumber) {
        HotNumberEntity entity = new HotNumberEntity();
        entity.setId(hotNumber.getId());
        entity.setNumberPattern(hotNumber.getNumberPattern());
        entity.setFrequencyCount(hotNumber.getFrequencyCount());
        entity.setCreatedAt(hotNumber.getCreatedAt());
        entity.setUpdatedAt(hotNumber.getUpdatedAt());
        
        if (hotNumber.getPatternGroupId() != null) {
            PatternGroupEntity patternGroup = patternGroupRepository.findById(hotNumber.getPatternGroupId())
                    .orElseThrow(() -> new RuntimeException("Pattern group not found: " + hotNumber.getPatternGroupId()));
            entity.setPatternGroup(patternGroup);
        }
        
        return entity;
    }
    
    private HotNumber convertToHotNumber(HotNumberEntity entity) {
        HotNumber hotNumber = new HotNumber();
        hotNumber.setId(entity.getId());
        hotNumber.setNumberPattern(entity.getNumberPattern());
        hotNumber.setFrequencyCount(entity.getFrequencyCount());
        hotNumber.setCreatedAt(entity.getCreatedAt());
        hotNumber.setUpdatedAt(entity.getUpdatedAt());
        hotNumber.setPatternGroupId(entity.getPatternGroup().getId());
        return hotNumber;
    }
    
    // Lottery Ticket Methods
    public LotteryTicket saveLotteryTicket(LotteryTicket ticket) {
        LotteryTicketEntity entity = convertToLotteryTicketEntity(ticket);
        LotteryTicketEntity savedEntity = lotteryTicketRepository.save(entity);
        return convertToLotteryTicket(savedEntity);
    }
    
    public List<LotteryTicket> findLotteryTicketsByTicketSetId(Long ticketSetId) {
        return lotteryTicketRepository.findByTicketSetId(ticketSetId).stream()
                .map(this::convertToLotteryTicket)
                .collect(Collectors.toList());
    }
    
    public List<LotteryTicket> findLotteryTicketsByPattern(String pattern) {
        return lotteryTicketRepository.findByPatternUsed(pattern).stream()
                .map(this::convertToLotteryTicket)
                .collect(Collectors.toList());
    }
    
    private LotteryTicketEntity convertToLotteryTicketEntity(LotteryTicket ticket) {
        LotteryTicketEntity entity = new LotteryTicketEntity();
        entity.setTicketId(ticket.getTicketId());
        entity.setTicketSetId(ticket.getTicketSetId());
        entity.setPatternUsed(ticket.getPatternUsed());
        entity.setConfidenceScore(ticket.getConfidenceScore());
        entity.setCreatedAt(ticket.getCreatedAt());
        
        // Convert List<Integer> to comma-separated string
        if (ticket.getNumbers() != null && !ticket.getNumbers().isEmpty()) {
            String numbersString = ticket.getNumbers().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            entity.setNumbers(numbersString);
        }
        
        // Convert metadata map to JSON string
        try {
            if (ticket.getMetadata() != null) {
                entity.setMetadata(objectMapper.writeValueAsString(ticket.getMetadata()));
            }
        } catch (JsonProcessingException e) {
            log.error("Error serializing ticket metadata", e);
        }
        
        return entity;
    }
    
    private LotteryTicket convertToLotteryTicket(LotteryTicketEntity entity) {
        LotteryTicket ticket = new LotteryTicket();
        ticket.setTicketId(entity.getTicketId());
        ticket.setTicketSetId(entity.getTicketSetId());
        ticket.setPatternUsed(entity.getPatternUsed());
        ticket.setConfidenceScore(entity.getConfidenceScore());
        ticket.setCreatedAt(entity.getCreatedAt());
        
        // Convert comma-separated string to List<Integer>
        if (entity.getNumbers() != null && !entity.getNumbers().trim().isEmpty()) {
            List<Integer> numbers = Arrays.stream(entity.getNumbers().split(","))
                    .map(String::trim)
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
            ticket.setNumbers(numbers);
        }
        
        // Convert JSON string to metadata map
        try {
            if (entity.getMetadata() != null) {
                Map<String, Object> metadata = objectMapper.readValue(entity.getMetadata(), Map.class);
                ticket.setMetadata(metadata);
            }
        } catch (JsonProcessingException e) {
            log.error("Error deserializing ticket metadata", e);
        }
        
        return ticket;
    }
    
    // Generated Ticket Set Methods
    public GeneratedTicketSet saveGeneratedTicketSet(GeneratedTicketSet ticketSet) {
        GeneratedTicketSetEntity entity = convertToGeneratedTicketSetEntity(ticketSet);
        GeneratedTicketSetEntity savedEntity = generatedTicketSetRepository.save(entity);
        return convertToGeneratedTicketSet(savedEntity);
    }
    
    public List<GeneratedTicketSet> findGeneratedTicketSetsBySessionId(String sessionId) {
        return generatedTicketSetRepository.findBySessionId(sessionId).stream()
                .map(this::convertToGeneratedTicketSet)
                .collect(Collectors.toList());
    }
    
    private GeneratedTicketSetEntity convertToGeneratedTicketSetEntity(GeneratedTicketSet ticketSet) {
        GeneratedTicketSetEntity entity = new GeneratedTicketSetEntity();
        entity.setSessionId(ticketSet.getSessionId());
        
        if (ticketSet.getQualityMetrics() != null) {
            entity.setDiversityScore(BigDecimal.valueOf(ticketSet.getQualityMetrics().getDiversityScore()));
            entity.setPatternCoverageScore(BigDecimal.valueOf(ticketSet.getQualityMetrics().getPatternCoverageScore()));
            entity.setExpectedHitRate(BigDecimal.valueOf(ticketSet.getQualityMetrics().getExpectedHitRate()));
        }
        
        if (ticketSet.getExpectedPerformance() != null) {
            entity.setOverallConfidence(BigDecimal.valueOf(ticketSet.getExpectedPerformance().getOverallConfidence()));
            entity.setRecommendationLevel(ticketSet.getExpectedPerformance().getRecommendationLevel());
        }
        
        try {
            if (ticketSet.getPatternsUsed() != null) {
                entity.setPatternsUsed(objectMapper.writeValueAsString(ticketSet.getPatternsUsed()));
            }
            if (ticketSet.getQualityMetrics() != null && ticketSet.getQualityMetrics().getTierProbabilities() != null) {
                entity.setTierProbabilities(objectMapper.writeValueAsString(ticketSet.getQualityMetrics().getTierProbabilities()));
            }
        } catch (JsonProcessingException e) {
            log.error("Error serializing ticket set data", e);
        }
        
        return entity;
    }
    
    private GeneratedTicketSet convertToGeneratedTicketSet(GeneratedTicketSetEntity entity) {
        GeneratedTicketSet ticketSet = new GeneratedTicketSet();
        ticketSet.setSessionId(entity.getSessionId());
        
        GeneratedTicketSet.QualityMetrics qualityMetrics = new GeneratedTicketSet.QualityMetrics();
        if (entity.getDiversityScore() != null) qualityMetrics.setDiversityScore(entity.getDiversityScore().doubleValue());
        if (entity.getPatternCoverageScore() != null) qualityMetrics.setPatternCoverageScore(entity.getPatternCoverageScore().doubleValue());
        if (entity.getExpectedHitRate() != null) qualityMetrics.setExpectedHitRate(entity.getExpectedHitRate().doubleValue());
        ticketSet.setQualityMetrics(qualityMetrics);
        
        GeneratedTicketSet.ExpectedPerformance expectedPerformance = new GeneratedTicketSet.ExpectedPerformance();
        if (entity.getOverallConfidence() != null) expectedPerformance.setOverallConfidence(entity.getOverallConfidence().doubleValue());
        expectedPerformance.setRecommendationLevel(entity.getRecommendationLevel());
        ticketSet.setExpectedPerformance(expectedPerformance);
        
        try {
            if (entity.getPatternsUsed() != null) {
                List<String> patterns = objectMapper.readValue(entity.getPatternsUsed(), List.class);
                ticketSet.setPatternsUsed(patterns);
            }
            if (entity.getTierProbabilities() != null) {
                Map<String, Double> tierProbs = objectMapper.readValue(entity.getTierProbabilities(), Map.class);
                qualityMetrics.setTierProbabilities(tierProbs);
            }
        } catch (JsonProcessingException e) {
            log.error("Error deserializing ticket set data", e);
        }
        
        return ticketSet;
    }
    
    // Lottery Draw Methods
    public LotteryDraw saveLotteryDraw(LotteryDraw draw) {
        LotteryDrawEntity entity = convertToLotteryDrawEntity(draw);
        LotteryDrawEntity savedEntity = lotteryDrawRepository.save(entity);
        
        // Cleanup old draws to maintain max 500 per config
        cleanupOldDraws(draw.getLotteryConfigId());
        
        return convertToLotteryDraw(savedEntity);
    }
    
    public List<LotteryDraw> findLotteryDrawsByConfigId(String lotteryConfigId, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        return lotteryDrawRepository.findByLotteryConfigurationIdOrderByDrawDateDesc(lotteryConfigId, pageable)
                .stream()
                .map(this::convertToLotteryDraw)
                .collect(Collectors.toList());
    }
    
    public Optional<LotteryDraw> findLatestLotteryDraw(String lotteryConfigId) {
        return lotteryDrawRepository.findLatestByLotteryConfigurationId(lotteryConfigId)
                .map(this::convertToLotteryDraw);
    }
    
    // Number Frequency Methods
    public NumberFrequency saveNumberFrequency(NumberFrequency frequency) {
        NumberFrequencyEntity entity = convertToNumberFrequencyEntity(frequency);
        NumberFrequencyEntity savedEntity = numberFrequencyRepository.save(entity);
        return convertToNumberFrequency(savedEntity);
    }
    
    public Optional<NumberFrequency> findNumberFrequencyByConfigId(String lotteryConfigId) {
        return numberFrequencyRepository.findByLotteryConfigurationId(lotteryConfigId)
                .map(this::convertToNumberFrequency);
    }
    
    private LotteryDrawEntity convertToLotteryDrawEntity(LotteryDraw draw) {
        LotteryDrawEntity entity = new LotteryDrawEntity();
        entity.setDrawId(draw.getDrawId());
        entity.setDrawDate(draw.getDrawDate());
        entity.setDrawNumber(draw.getDrawNumber());
        
        if (draw.getNumbers() != null && !draw.getNumbers().isEmpty()) {
            String numbersString = draw.getNumbers().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            entity.setNumbers(numbersString);
        }
        
        if (draw.getLotteryConfigId() != null) {
            LotteryConfigurationEntity lotteryConfig = lotteryConfigRepository.findById(draw.getLotteryConfigId())
                    .orElseThrow(() -> new RuntimeException("Lottery configuration not found: " + draw.getLotteryConfigId()));
            entity.setLotteryConfiguration(lotteryConfig);
        }
        
        return entity;
    }
    
    private LotteryDraw convertToLotteryDraw(LotteryDrawEntity entity) {
        LotteryDraw draw = new LotteryDraw();
        draw.setDrawId(entity.getDrawId());
        draw.setDrawDate(entity.getDrawDate());
        draw.setDrawNumber(entity.getDrawNumber());
        draw.setLotteryConfigId(entity.getLotteryConfiguration().getId());
        
        if (entity.getNumbers() != null && !entity.getNumbers().trim().isEmpty()) {
            List<Integer> numbers = Arrays.stream(entity.getNumbers().split(","))
                    .map(String::trim)
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
            draw.setNumbers(numbers);
        }
        
        return draw;
    }
    
    private NumberFrequencyEntity convertToNumberFrequencyEntity(NumberFrequency frequency) {
        NumberFrequencyEntity entity = new NumberFrequencyEntity();
        entity.setId(frequency.getId());
        entity.setTotalDrawsAnalyzed(frequency.getTotalDrawsAnalyzed());
        entity.setLastUpdated(frequency.getLastUpdated());
        
        if (frequency.getLotteryConfigId() != null) {
            LotteryConfigurationEntity lotteryConfig = lotteryConfigRepository.findById(frequency.getLotteryConfigId())
                    .orElseThrow(() -> new RuntimeException("Lottery configuration not found: " + frequency.getLotteryConfigId()));
            entity.setLotteryConfiguration(lotteryConfig);
        }
        
        try {
            if (frequency.getFrequencies() != null) {
                entity.setFrequencies(objectMapper.writeValueAsString(frequency.getFrequencies()));
            }
        } catch (JsonProcessingException e) {
            log.error("Error serializing frequencies", e);
        }
        
        return entity;
    }
    
    private NumberFrequency convertToNumberFrequency(NumberFrequencyEntity entity) {
        NumberFrequency frequency = new NumberFrequency();
        frequency.setId(entity.getId());
        frequency.setLotteryConfigId(entity.getLotteryConfiguration().getId());
        frequency.setTotalDrawsAnalyzed(entity.getTotalDrawsAnalyzed());
        frequency.setLastUpdated(entity.getLastUpdated());
        
        try {
            if (entity.getFrequencies() != null) {
                Map<String, Double> frequencies = objectMapper.readValue(entity.getFrequencies(), Map.class);
                frequency.setFrequencies(frequencies);
            }
        } catch (JsonProcessingException e) {
            log.error("Error deserializing frequencies", e);
        }
        
        return frequency;
    }
    
    // Cleanup Methods
    private void cleanupOldDraws(String lotteryConfigId) {
        try {
            Long totalDraws = lotteryDrawRepository.countByLotteryConfigurationId(lotteryConfigId);
            
            if (totalDraws > 500) {
                int deletedCount = lotteryDrawRepository.deleteExcessDraws(lotteryConfigId);
                if (deletedCount > 0) {
                    log.info("Cleaned up {} old draws for lottery config: {}", deletedCount, lotteryConfigId);
                }
            }
        } catch (Exception e) {
            log.warn("Cleanup failed for lottery config: {} - {}", lotteryConfigId, e.getMessage());
        }
    }
    
    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
    @Transactional
    public void cleanupAllLotteryDraws() {
        log.info("Starting scheduled cleanup of lottery draws");
        
        List<LotteryConfiguration> allConfigs = findAllLotteryConfigurations();
        for (LotteryConfiguration config : allConfigs) {
            cleanupOldDraws(config.getId());
        }
        
        log.info("Completed scheduled cleanup of lottery draws");
    }
}