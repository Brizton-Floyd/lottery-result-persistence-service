package com.floyd.lottoptions.persistence.repository;

import com.floyd.lottoptions.persistence.entity.LotteryTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LotteryTicketRepository extends JpaRepository<LotteryTicketEntity, String> {
    
    List<LotteryTicketEntity> findByTicketSetId(Long ticketSetId);
    
    @Query("SELECT lt FROM LotteryTicketEntity lt WHERE lt.patternUsed = :pattern")
    List<LotteryTicketEntity> findByPatternUsed(@Param("pattern") String pattern);
    
    @Query("SELECT lt FROM LotteryTicketEntity lt WHERE lt.ticketSetId IN :ticketSetIds")
    List<LotteryTicketEntity> findByTicketSetIds(@Param("ticketSetIds") List<Long> ticketSetIds);
}