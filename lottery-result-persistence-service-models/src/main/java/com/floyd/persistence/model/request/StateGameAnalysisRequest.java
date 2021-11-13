package com.floyd.persistence.model.request;

import lombok.Data;

@Data
public class StateGameAnalysisRequest {
    private String stateName;
    private String gameName;
    private String analysisMethod;
    private String drawPosition;
    private String gameOutRange;
    private boolean generateDrawNumbers;
    private int numbersToGeneratePerPattern;
    private String[] drawLowHighPatterns;
    private int[] patternSegments;
}
