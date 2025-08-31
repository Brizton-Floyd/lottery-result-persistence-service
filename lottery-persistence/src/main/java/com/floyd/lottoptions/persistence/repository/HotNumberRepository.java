package com.floyd.lottoptions.persistence.repository;

import com.floyd.lottoptions.persistence.entity.HotNumberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotNumberRepository extends JpaRepository<HotNumberEntity, Long> {
    
    List<HotNumberEntity> findByPatternGroupId(Long patternGroupId);
    
    @Query("SELECT hn FROM HotNumberEntity hn JOIN hn.patternGroup pg WHERE pg.lotteryConfiguration.id = :lotteryConfigId")
    List<HotNumberEntity> findByLotteryConfigId(@Param("lotteryConfigId") String lotteryConfigId);
    
    @Query("SELECT hn FROM HotNumberEntity hn JOIN hn.patternGroup pg WHERE pg.lotteryConfiguration.id = :lotteryConfigId AND pg.patternType = :patternType")
    List<HotNumberEntity> findByLotteryConfigIdAndPatternType(
        @Param("lotteryConfigId") String lotteryConfigId, 
        @Param("patternType") String patternType);
    
    Optional<HotNumberEntity> findByPatternGroupIdAndNumberPattern(Long patternGroupId, String numberPattern);
}