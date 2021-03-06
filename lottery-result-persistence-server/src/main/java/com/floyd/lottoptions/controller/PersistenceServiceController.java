package com.floyd.lottoptions.controller;

import com.floyd.lottoptions.agr.service.impl.LotteryResultPollingService;
import com.floyd.lottoptions.service.DataService;
import com.floyd.persistence.model.request.StateGameAnalysisRequest;
import com.floyd.persistence.model.response.AllStateLottoGameResponse;
import com.floyd.persistence.model.response.StateGamesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class PersistenceServiceController {
    private static final Logger log = LoggerFactory.getLogger(LotteryResultPollingService.class);

    private final DataService lotteryDataService;

    public PersistenceServiceController(DataService lotteryDataService) {
        this.lotteryDataService = lotteryDataService;
    }

    @GetMapping("/all/state-games")
    ResponseEntity<AllStateLottoGameResponse> getAllUsStateGames() {
        log.info("Received request for all state lotto games");
        try {
            final Optional<AllStateLottoGameResponse> stateData = lotteryDataService.getAllStateLotteryGames();
            if (stateData.isPresent()) {
                return new ResponseEntity<>(stateData.get(), HttpStatus.OK);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/state/games")
    ResponseEntity<StateGamesResponse> requestResponse(@Validated @RequestBody StateGameAnalysisRequest request) {
        log.info("Received request: {}", request);
        try {
            final Optional<StateGamesResponse> stateData = lotteryDataService.getStateData(request);
            if (stateData.isPresent()) {
                return new ResponseEntity<>(stateData.get(), HttpStatus.OK);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }
}
