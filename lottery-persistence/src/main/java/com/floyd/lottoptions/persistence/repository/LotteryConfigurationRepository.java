package com.floyd.lottoptions.persistence.repository;

import com.floyd.lottoptions.persistence.entity.LotteryConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LotteryConfigurationRepository extends JpaRepository<LotteryConfigurationEntity, String> {
    
    Optional<LotteryConfigurationEntity> findByName(String name);
    
    @Query("SELECT lc FROM LotteryConfigurationEntity lc WHERE lc.drawSize = :drawSize")
    List<LotteryConfigurationEntity> findByDrawSize(@Param("drawSize") Integer drawSize);
    
    @Query("SELECT lc FROM LotteryConfigurationEntity lc WHERE lc.minNumber >= :minNumber AND lc.maxNumber <= :maxNumber")
    List<LotteryConfigurationEntity> findByNumberRange(@Param("minNumber") Integer minNumber, @Param("maxNumber") Integer maxNumber);
}