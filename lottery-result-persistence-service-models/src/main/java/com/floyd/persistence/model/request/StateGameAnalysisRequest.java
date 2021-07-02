package com.floyd.persistence.model.request;

import lombok.Data;

@Data
public class StateGameAnalysisRequest {
    private String stateName;
    private String gameName;
    private String analysisMethod;
    private String drawPosition;
    private String lottoNumberGroupRange;
}
