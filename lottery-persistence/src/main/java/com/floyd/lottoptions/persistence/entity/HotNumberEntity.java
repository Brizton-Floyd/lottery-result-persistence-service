package com.floyd.lottoptions.persistence.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hot_numbers")
@Data
@EqualsAndHashCode(of = "id")
public class HotNumberEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "number_pattern", nullable = false, length = 100)
    private String numberPattern;
    
    @Column(name = "frequency_count")
    private Integer frequencyCount = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pattern_group_id", nullable = false)
    private PatternGroupEntity patternGroup;
    
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