package com.floyd.lottoptions.persistence.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pattern_groups")
@Data
@EqualsAndHashCode(of = "id")
public class PatternGroupEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "pattern_type", nullable = false)
    private PatternType patternType;
    
    @Column(name = "efficiency_multiplier", precision = 5, scale = 4)
    private BigDecimal efficiencyMultiplier = BigDecimal.ONE;
    
    @Column(name = "total_analyzed_draws")
    private Integer totalAnalyzedDraws = 0;
    
    @Column(name = "last_analysis_date")
    private LocalDateTime lastAnalysisDate;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lottery_config_id", nullable = false)
    private LotteryConfigurationEntity lotteryConfiguration;
    
    @OneToMany(mappedBy = "patternGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HotNumberEntity> hotNumbers;
    
    public enum PatternType {
        HOT, WARM, COLD
    }
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
        if (lastAnalysisDate == null) {
            lastAnalysisDate = LocalDateTime.now();
        }
    }
}