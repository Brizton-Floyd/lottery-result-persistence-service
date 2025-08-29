package com.floyd.lottoptions.persistence.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "lottery_configurations")
@Data
@EqualsAndHashCode(of = "id")
public class LotteryConfigurationEntity {
    
    @Id
    @Column(length = 50)
    private String id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(name = "min_number", nullable = false)
    private Integer minNumber;
    
    @Column(name = "max_number", nullable = false)
    private Integer maxNumber;
    
    @Column(name = "draw_size", nullable = false)
    private Integer drawSize;
    
    @Column(name = "pattern_length", nullable = false)
    private Integer patternLength;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "lotteryConfiguration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PrizeStructureEntity> prizeStructures;
    
    @OneToMany(mappedBy = "lotteryConfiguration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PatternGroupEntity> patternGroups;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}