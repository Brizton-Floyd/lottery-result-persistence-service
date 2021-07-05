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
    List<LotteryDraw> lotteryDraws = new ArrayList<>();

    @Override
    public String toString() {
        return this.fullName;
    }
}
