package com.floyd.lottoptions.agr.model;

import lombok.Data;

import java.util.List;

@Data
public class Prizes {
    private String asOfDate;
    private List<Values> values;
}
