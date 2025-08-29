package com.floyd.lottoptions.persistence.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;

@Entity
@Table(name = "prize_structures")
@Data
@EqualsAndHashCode(of = "id")
public class PrizeStructureEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tier_name", nullable = false, length = 20)
    private String tierName;
    
    @Column(name = "match_count", nullable = false)
    private Integer matchCount;
    
    @Column(length = 255)
    private String description;
    
    @Column(name = "is_active")
    private Boolean active = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lottery_config_id", nullable = false)
    private LotteryConfigurationEntity lotteryConfiguration;
}