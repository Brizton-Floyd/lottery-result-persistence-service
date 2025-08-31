package com.floyd.lottoptions.persistence.repository;

import com.floyd.lottoptions.persistence.entity.NumberFrequencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NumberFrequencyRepository extends JpaRepository<NumberFrequencyEntity, Long> {
    
    @Query("SELECT nf FROM NumberFrequencyEntity nf WHERE nf.lotteryConfiguration.id = :lotteryConfigId")
    Optional<NumberFrequencyEntity> findByLotteryConfigurationId(@Param("lotteryConfigId") String lotteryConfigId);
    
    void deleteByLotteryConfigurationId(String lotteryConfigId);
}