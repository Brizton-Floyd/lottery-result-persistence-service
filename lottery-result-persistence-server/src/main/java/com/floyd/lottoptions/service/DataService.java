package com.floyd.lottoptions.service;

import com.floyd.persistence.model.request.StateGameAnalysisRequest;
import com.floyd.persistence.model.response.StateGamesResponse;

import java.util.Optional;

public interface DataService {
    Optional<StateGamesResponse> getStateData(StateGameAnalysisRequest stateGameAnalysisRequest) throws Exception;
}
