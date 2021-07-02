package com.floyd.persistence.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class LotteryDraw implements Serializable {
    private LocalDate drawDate;
    private List<Integer> drawResults = new ArrayList<>();
}
