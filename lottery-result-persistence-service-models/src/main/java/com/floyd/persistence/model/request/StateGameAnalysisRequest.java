package com.floyd.persistence.model.request;

import lombok.Data;

@Data
public class StateGameAnalysisRequest {
    private String region;
    private String gameId;
    private String analysisMethod;
    private String drawPosition;
    private String lottoNumberGroupRange;
}
