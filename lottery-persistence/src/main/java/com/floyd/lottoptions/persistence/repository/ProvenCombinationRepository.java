package com.floyd.lottoptions.persistence.repository;

import com.floyd.lottoptions.persistence.entity.ProvenCombinationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProvenCombinationRepository extends JpaRepository<ProvenCombinationEntity, Long> {
    
    List<ProvenCombinationEntity> findByLotteryConfigurationId(String lotteryConfigId);
    
    Optional<ProvenCombinationEntity> findByLotteryConfigurationIdAndCombinationNumbers(
        String lotteryConfigId, String combinationNumbers);
    
    @Query("SELECT pc FROM ProvenCombinationEntity pc WHERE pc.lotteryConfiguration.id = :lotteryConfigId AND pc.qualityScore >= :minScore ORDER BY pc.qualityScore DESC")
    List<ProvenCombinationEntity> findHighQualityCombinations(
        @Param("lotteryConfigId") String lotteryConfigId, 
        @Param("minScore") BigDecimal minScore);
    
    @Query("SELECT pc FROM ProvenCombinationEntity pc WHERE pc.lotteryConfiguration.id = :lotteryConfigId AND pc.winCount > 0 ORDER BY pc.winCount DESC")
    List<ProvenCombinationEntity> findWinningCombinations(@Param("lotteryConfigId") String lotteryConfigId);
    
    @Query("SELECT pc FROM ProvenCombinationEntity pc WHERE pc.lotteryConfiguration.id = :lotteryConfigId ORDER BY pc.frequencyCount DESC")
    List<ProvenCombinationEntity> findMostFrequentCombinations(@Param("lotteryConfigId") String lotteryConfigId);
    
    void deleteByLotteryConfigurationId(String lotteryConfigId);
}