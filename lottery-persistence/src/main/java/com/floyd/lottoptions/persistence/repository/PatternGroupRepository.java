package com.floyd.lottoptions.persistence.repository;

import com.floyd.lottoptions.persistence.entity.PatternGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatternGroupRepository extends JpaRepository<PatternGroupEntity, Long> {
    
    List<PatternGroupEntity> findByLotteryConfigurationId(String lotteryConfigId);
    
    Optional<PatternGroupEntity> findByLotteryConfigurationIdAndPatternType(
        String lotteryConfigId, 
        PatternGroupEntity.PatternType patternType
    );
    
    @Query("SELECT pg FROM PatternGroupEntity pg WHERE pg.lotteryConfiguration.id = :lotteryConfigId AND pg.patternType = :patternType")
    Optional<PatternGroupEntity> findPatternGroup(@Param("lotteryConfigId") String lotteryConfigId, 
                                                  @Param("patternType") PatternGroupEntity.PatternType patternType);
    
    @Query("SELECT pg FROM PatternGroupEntity pg WHERE pg.efficiencyMultiplier > :minMultiplier ORDER BY pg.efficiencyMultiplier DESC")
    List<PatternGroupEntity> findHighEfficiencyPatterns(@Param("minMultiplier") Double minMultiplier);
    
    void deleteByLotteryConfigurationId(String lotteryConfigId);
}