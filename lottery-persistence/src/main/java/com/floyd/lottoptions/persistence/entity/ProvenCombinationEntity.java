package com.floyd.lottoptions.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proven_combinations")
@Data
public class ProvenCombinationEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lottery_configuration_id", nullable = false)
    private LotteryConfigurationEntity lotteryConfiguration;
    
    @Column(name = "combination_numbers", nullable = false)
    private String combinationNumbers; // Comma-separated string: "7,14,21,35,42,49"
    
    @Column(name = "frequency_count")
    private Integer frequencyCount;
    
    @Column(name = "last_drawn_date")
    private LocalDate lastDrawnDate;
    
    @Column(name = "win_count")
    private Integer winCount;
    
    @Column(name = "tier_performance", length = 500)
    private String tierPerformance; // JSON string: {"tier_1": 2, "tier_2": 5}
    
    @Column(name = "quality_score", precision = 5, scale = 2)
    private BigDecimal qualityScore;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}