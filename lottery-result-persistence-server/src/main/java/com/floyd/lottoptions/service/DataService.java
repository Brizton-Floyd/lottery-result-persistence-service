package com.floyd.lottoptions.service;

import model.request.StateGamesRequest;
import model.response.StateGamesResponse;

import java.util.Optional;

public interface DataService {
    Optional<StateGamesResponse> getStateData(StateGamesRequest stateGamesRequest) throws Exception;
}
