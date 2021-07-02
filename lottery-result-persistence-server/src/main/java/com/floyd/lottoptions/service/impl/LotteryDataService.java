//package com.floyd.lottoptions.service.impl;
//
//import com.floyd.lottoptions.service.DataService;
//import com.floyd.persistence.model.LotteryGame;
//import com.floyd.persistence.model.LotteryState;
//import com.floyd.persistence.model.request.StateGameAnalysisRequest;
//import com.floyd.persistence.model.response.StateGamesResponse;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.stereotype.Service;
//import org.springframework.util.StringUtils;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//@Service
//public class LotteryDataService implements DataService {
//
//    private final MongoConfig mongoConfig;
//
//    public LotteryDataService(MongoConfig mongoConfig) {
//        this.mongoConfig = mongoConfig;
//    }
//
//    @Override
//    public Optional<StateGamesResponse> getStateData(StateGameAnalysisRequest stateGameAnalysisRequest) throws Exception {
//        int idx = 0;
//        StateGamesResponse stateGamesResponse = null;
//        Optional<LotteryState> lotteryState = Optional.ofNullable(mongoConfig.mongoTemplate()
//                .findOne(findOne
//                        Query.query(
//                                Criteria.where("stateRegion").is(stateGameAnalysisRequest.getRegion())), LotteryState.class));
//
//        if (lotteryState.isPresent()) {
//
//            List<LotteryGame> lotteryGames = new ArrayList<>();
//            stateGamesResponse = new StateGamesResponse();
//            final List<LotteryGame> stateLotteryGames = lotteryState.get().getStateLotteryGames();
//
//            // Return just the chosen game based on ID provided in the request
//            if (StringUtils.hasText(stateGameAnalysisRequest.getGameId())) {
//                LotteryGame filteredLotteryGame = stateLotteryGames
//                .stream()
//                .filter(game -> game.getId().equals(stateGameAnalysisRequest.getGameId()))
//                .findFirst()
//                .orElse(null);
//                stateGamesResponse.setLotteryGame(filteredLotteryGame);
//            }
//            else {
//                // Return all given games the current state runs
//                for (LotteryGame lotteryGame : stateLotteryGames) {
//                    if (idx++ != 0) {
//                        lotteryGame.setLotteryDraws(new ArrayList<>());
//                    }
//                    lotteryGames.add(lotteryGame);
//                }
//            }
//            stateGamesResponse.setLotteryGames(lotteryGames);
//        }
//        return Optional.ofNullable(stateGamesResponse);
//    }
//}