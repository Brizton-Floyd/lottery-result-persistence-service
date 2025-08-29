package com.floyd.lottoptions.persistence.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Data
@EqualsAndHashCode(of = "sessionId")
public class UserSessionEntity {
    
    @Id
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    @Column(name = "target_tier", nullable = false, length = 20)
    private String targetTier;
    
    @Column(name = "number_of_tickets", nullable = false)
    private Integer numberOfTickets;
    
    @Column(name = "generation_strategy", nullable = false, length = 50)
    private String generationStrategy;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal budget;
    
    @Column(columnDefinition = "TEXT")
    private String preferences;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lottery_config_id", nullable = false)
    private LotteryConfigurationEntity lotteryConfiguration;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}