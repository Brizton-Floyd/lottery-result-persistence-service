package com.floyd.persistence.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LotteryGame implements Serializable {
    private String fullName;
    private String stateGameBelongsTo;
    private LocalDate dateLastUpdated;
    private Integer drawHistoryCount;
    private Integer drawPositionCount;
    private Integer maxNumber;
    private Integer minNumber;
    List<LotteryDraw> lotteryDraws = new ArrayList<>();

    @Override
    public String toString() {
        return this.fullName;
    }

    public void findMinMaxLottoNumber() {
        int currentMin = Integer.MAX_VALUE;
        int currentMax = Integer.MIN_VALUE;

        for (LotteryDraw lotteryDraw : lotteryDraws) {
            for (int winningNumber : lotteryDraw.getDrawResults()) {
                currentMin = Math.min(currentMin, winningNumber);
                currentMax = Math.max(currentMax, winningNumber);
            }
        }

        maxNumber = currentMax;
        minNumber = currentMin;
    }
}
