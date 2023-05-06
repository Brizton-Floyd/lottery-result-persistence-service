package com.floyd.persistence.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
public class LotteryState {
    private String stateRegion;
    private List<LotteryGame> stateLotteryGames;
}
