package com.floyd.lottoptions.persistence.repository;

import com.floyd.lottoptions.persistence.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSessionEntity, String> {
    
    List<UserSessionEntity> findByLotteryConfigurationId(String lotteryConfigId);
    
    List<UserSessionEntity> findByTargetTier(String targetTier);
    
    @Query("SELECT us FROM UserSessionEntity us WHERE us.createdAt >= :startDate ORDER BY us.createdAt DESC")
    List<UserSessionEntity> findRecentSessions(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT us FROM UserSessionEntity us WHERE us.lotteryConfiguration.id = :lotteryConfigId AND us.targetTier = :targetTier")
    List<UserSessionEntity> findByLotteryConfigAndTier(@Param("lotteryConfigId") String lotteryConfigId, 
                                                       @Param("targetTier") String targetTier);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSessionEntity us WHERE us.createdAt < :cutoffDate")
    int deleteSessionsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}