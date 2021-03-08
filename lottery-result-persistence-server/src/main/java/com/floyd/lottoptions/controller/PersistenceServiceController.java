package com.floyd.lottoptions.controller;

import com.floyd.lottoptions.agr.service.impl.LotteryResultPollingService;
import com.floyd.lottoptions.service.DataService;
import model.request.StateGamesRequest;
import model.response.StateGamesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class PersistenceServiceController {
    private static final Logger log = LoggerFactory.getLogger(LotteryResultPollingService.class);

    private DataService lotteryDataService;

    public PersistenceServiceController(DataService lotteryDataService) {
        this.lotteryDataService = lotteryDataService;
    }

    @PostMapping("/state/games")
    StateGamesResponse requestResponse(@Validated @RequestBody StateGamesRequest request) {
        log.info("Received request: {}", request);
        try {
            final Optional<StateGamesResponse> stateData = lotteryDataService.getStateData(request);
            if (stateData.isPresent()) {
                return stateData.get();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new StateGamesResponse();
    }
}
