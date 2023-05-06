package com.floyd.persistence.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LotteryDraw implements Serializable {
    private LocalDate drawDate;
    private Integer bonusNumber;
    private List<Integer> drawResults = new ArrayList<>();

    public LotteryDraw() {}
    public LotteryDraw(LotteryDraw lotteryDraw) {
        this.drawDate = lotteryDraw.getDrawDate();
        this.bonusNumber = lotteryDraw.getBonusNumber();
        populateDrawResults(lotteryDraw.getDrawResults());
    }

    private void populateDrawResults(List<Integer> drawResults) {
        for (int i = 0; i < drawResults.size(); i++) {
            this.drawResults.add(drawResults.get(i));
        }
    }
}
