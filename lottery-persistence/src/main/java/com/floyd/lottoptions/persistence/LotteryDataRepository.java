package com.floyd.lottoptions.persistence;

import com.floyd.lottoptions.datamodels.LotteryConfiguration;
import com.floyd.lottoptions.datamodels.PatternGroupDefinition;
import com.floyd.lottoptions.datamodels.UserRequest;
import com.floyd.lottoptions.datamodels.GeneratedTicketSet;
import com.floyd.lottoptions.datamodels.impl.DefaultLotteryConfiguration;
import com.floyd.lottoptions.datamodels.impl.DefaultPatternGroupDefinition;
import com.floyd.lottoptions.persistence.entity.LotteryConfigurationEntity;
import com.floyd.lottoptions.persistence.entity.PatternGroupEntity;
import com.floyd.lottoptions.persistence.entity.UserSessionEntity;
import com.floyd.lottoptions.persistence.repository.LotteryConfigurationRepository;
import com.floyd.lottoptions.persistence.repository.PatternGroupRepository;
import com.floyd.lottoptions.persistence.repository.UserSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
}