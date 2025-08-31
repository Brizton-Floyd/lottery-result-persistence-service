package com.floyd.lottoptions.datamodels;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class LotteryDraw {
    private Long drawId;
    private LocalDate drawDate;
    private Integer drawNumber;
    private List<Integer> numbers;
    private String lotteryConfigId;
}