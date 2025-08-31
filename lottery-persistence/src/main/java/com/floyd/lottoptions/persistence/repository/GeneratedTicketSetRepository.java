package com.floyd.lottoptions.persistence.repository;

import com.floyd.lottoptions.persistence.entity.GeneratedTicketSetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratedTicketSetRepository extends JpaRepository<GeneratedTicketSetEntity, Long> {
    
    List<GeneratedTicketSetEntity> findBySessionId(String sessionId);
    
    @Query("SELECT gts FROM GeneratedTicketSetEntity gts WHERE gts.recommendationLevel = :level ORDER BY gts.overallConfidence DESC")
    List<GeneratedTicketSetEntity> findByRecommendationLevel(@Param("level") String level);
    
    @Query("SELECT gts FROM GeneratedTicketSetEntity gts WHERE gts.overallConfidence >= :minConfidence ORDER BY gts.overallConfidence DESC")
    List<GeneratedTicketSetEntity> findHighConfidenceTicketSets(@Param("minConfidence") double minConfidence);
}