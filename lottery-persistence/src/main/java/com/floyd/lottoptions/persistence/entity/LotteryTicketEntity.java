package com.floyd.lottoptions.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lottery_tickets")
@Data
public class LotteryTicketEntity {
    
    @Id
    @Column(name = "ticket_id", length = 100)
    private String ticketId;
    
    @Column(name = "ticket_set_id", nullable = false)
    private Long ticketSetId;
    
    @Column(name = "numbers", nullable = false)
    private String numbers; // Comma-separated numbers
    
    @Column(name = "pattern_used", length = 100)
    private String patternUsed;
    
    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}