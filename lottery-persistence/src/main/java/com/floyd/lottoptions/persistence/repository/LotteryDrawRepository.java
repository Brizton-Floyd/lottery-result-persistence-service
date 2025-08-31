package com.floyd.lottoptions.persistence.repository;

import com.floyd.lottoptions.persistence.entity.LotteryDrawEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LotteryDrawRepository extends JpaRepository<LotteryDrawEntity, Long> {
    
    @Query("SELECT ld FROM LotteryDrawEntity ld WHERE ld.lotteryConfiguration.id = :lotteryConfigId ORDER BY ld.drawDate DESC, ld.drawNumber DESC")
    List<LotteryDrawEntity> findByLotteryConfigurationIdOrderByDrawDateDesc(
            @Param("lotteryConfigId") String lotteryConfigId, Pageable pageable);
    
    List<LotteryDrawEntity> findByLotteryConfigurationIdOrderByDrawDateDesc(String lotteryConfigId);
    
    @Query("SELECT COUNT(ld) FROM LotteryDrawEntity ld WHERE ld.lotteryConfiguration.id = :lotteryConfigId")
    Long countByLotteryConfigurationId(@Param("lotteryConfigId") String lotteryConfigId);
    
    @Query("SELECT ld FROM LotteryDrawEntity ld WHERE ld.lotteryConfiguration.id = :lotteryConfigId ORDER BY ld.drawDate DESC, ld.drawNumber DESC LIMIT 1")
    Optional<LotteryDrawEntity> findLatestByLotteryConfigurationId(@Param("lotteryConfigId") String lotteryConfigId);
    
    @Modifying
    @Query(value = "DELETE FROM lottery_draws WHERE lottery_configuration_id = :lotteryConfigId AND draw_id NOT IN (SELECT draw_id FROM (SELECT draw_id FROM lottery_draws WHERE lottery_configuration_id = :lotteryConfigId ORDER BY draw_date DESC, draw_number DESC LIMIT 500) AS recent_draws)", nativeQuery = true)
    int deleteExcessDraws(@Param("lotteryConfigId") String lotteryConfigId);
}