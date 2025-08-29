package com.floyd.lottoptions.controller;

import com.floyd.lottoptions.agr.polling.LotteryResultPollingService;
import com.floyd.lottoptions.service.DataService;
import com.floyd.persistence.model.LotteryGame;
import com.floyd.persistence.model.LotteryState;
import com.floyd.persistence.model.request.StateGameAnalysisRequest;
import com.floyd.persistence.model.response.AllStateLottoGameResponse;
import com.floyd.persistence.model.response.StateGamesResponse;
import com.floyd.persistence.model.response.StateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class PersistenceServiceController {
    private static final Logger log = LoggerFactory.getLogger(LotteryResultPollingService.class);

    private final DataService lotteryDataService;

    public PersistenceServiceController(DataService lotteryDataService) {
        this.lotteryDataService = lotteryDataService;
    }

    @GetMapping("/states")
    public Flux<StateResponse> getLotteryStates() {
        List<LotteryState> states = lotteryDataService
                .fetchStates();

        StateResponse stateResponse = new StateResponse();
        stateResponse.setData(states);

        return Flux.just(stateResponse);
    }

    @GetMapping("/states/{stateName}/games")
    public Flux<LotteryGame> getGames(@PathVariable String stateName) {
        List<LotteryGame> lotteryGames = lotteryDataService
                .fetchStateGames(stateName);

        return Flux.fromIterable(lotteryGames);
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

    @GetMapping("/all/v2/state-games")
    ResponseEntity<AllStateLottoGameResponse> getAllUsStateGamesV2() {
        log.info("Received request for all state lotto games");
        try {
            final Optional<AllStateLottoGameResponse> stateData = lotteryDataService.getAllStateLotteryGamesV2();
            if (stateData.isPresent()) {
                return new ResponseEntity<>(stateData.get(), HttpStatus.OK);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/state-games/{state}")
    ResponseEntity<List<String>> getAllStateGames(@PathVariable String state) {
        log.info("Received request for all state lotto games");
        try {
            final Optional<List<String>> stateData = lotteryDataService.getAllStateLotteryGames(state);
            if (stateData.isPresent()) {
                return new ResponseEntity<>(stateData.get(), HttpStatus.OK);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/state/games")
    ResponseEntity<StateGamesResponse> requestResponse(@RequestBody StateGameAnalysisRequest request) {
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
