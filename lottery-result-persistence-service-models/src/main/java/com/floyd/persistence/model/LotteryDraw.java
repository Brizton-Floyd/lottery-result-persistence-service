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
}
