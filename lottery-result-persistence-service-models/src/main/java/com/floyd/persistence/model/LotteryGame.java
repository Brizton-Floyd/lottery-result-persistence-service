package com.floyd.persistence.model;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class LotteryGame implements Serializable {
    private String fullName;
    private Integer drawHistoryCount;
    List<LotteryDraw> lotteryDraws = new ArrayList<>();
}
