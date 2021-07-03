package com.floyd.persistence.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class LotteryGame implements Serializable {
    private String fullName;
    private LocalDate dateLastUpdated;
    private Integer drawHistoryCount;
    private Integer drawPositionCount;
    List<LotteryDraw> lotteryDraws = new ArrayList<>();
}
