package com.floyd.lottoptions.agr.service;

public interface PollingService {
    void pollForUpdatesToStateGames() throws Exception;
    void pollForUpdatesToDrawResults() throws Exception;
}
