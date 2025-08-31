package com.floyd.lottoptions.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "lottery_draws")
@Data
public class LotteryDrawEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long drawId;
    
    @Column(name = "draw_date", nullable = false)
    private LocalDate drawDate;
    
    @Column(name = "draw_number")
    private Integer drawNumber;
    
    @Column(name = "numbers", nullable = false)
    private String numbers; // Comma-separated numbers
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lottery_configuration_id", nullable = false)
    private LotteryConfigurationEntity lotteryConfiguration;
}