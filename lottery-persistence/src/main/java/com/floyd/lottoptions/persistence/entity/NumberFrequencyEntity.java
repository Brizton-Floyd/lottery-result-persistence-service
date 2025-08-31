package com.floyd.lottoptions.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "number_frequencies")
@Data
public class NumberFrequencyEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lottery_configuration_id", nullable = false)
    private LotteryConfigurationEntity lotteryConfiguration;
    
    @Column(name = "frequencies", columnDefinition = "TEXT", nullable = false)
    private String frequencies; // JSON string: {"1": 0.0285, "2": 0.0312}
    
    @Column(name = "total_draws_analyzed")
    private Integer totalDrawsAnalyzed;
    
    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}