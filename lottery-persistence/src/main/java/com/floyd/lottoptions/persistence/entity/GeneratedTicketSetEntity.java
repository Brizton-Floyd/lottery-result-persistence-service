package com.floyd.lottoptions.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "generated_ticket_sets")
@Data
public class GeneratedTicketSetEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;
    
    @Column(name = "diversity_score", precision = 5, scale = 4)
    private BigDecimal diversityScore;
    
    @Column(name = "pattern_coverage_score", precision = 5, scale = 4)
    private BigDecimal patternCoverageScore;
    
    @Column(name = "expected_hit_rate", precision = 5, scale = 4)
    private BigDecimal expectedHitRate;
    
    @Column(name = "overall_confidence", precision = 5, scale = 4)
    private BigDecimal overallConfidence;
    
    @Column(name = "recommendation_level", length = 20)
    private String recommendationLevel;
    
    @Column(name = "patterns_used", columnDefinition = "TEXT")
    private String patternsUsed; // JSON string
    
    @Column(name = "tier_probabilities", columnDefinition = "TEXT")
    private String tierProbabilities; // JSON string
    
    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;
}