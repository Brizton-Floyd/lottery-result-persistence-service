package com.floyd.lottoptions.service.impl;

import com.floyd.lottoptions.agr.config.MongoConfig;
import com.floyd.lottoptions.service.DataService;
import model.LotteryGame;
import model.LotteryState;
import model.request.StateGamesRequest;
import model.response.StateGamesResponse;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LotteryDataService implements DataService {

    private final MongoConfig mongoConfig;

    public LotteryDataService(MongoConfig mongoConfig) {
        this.mongoConfig = mongoConfig;
    }

    @Override
    public Optional<StateGamesResponse> getStateData(StateGamesRequest stateGamesRequest) throws Exception {
        int idx = 0;
        StateGamesResponse stateGamesResponse = null;
        Optional<LotteryState> lotteryState = Optional.ofNullable(mongoConfig.mongoTemplate()
                .findOne(
                        Query.query(
                                Criteria.where("stateRegion").is(stateGamesRequest.getRegion())), LotteryState.class));
        if (lotteryState.isPresent()) {
            final List<LotteryGame> stateLotteryGames = lotteryState.get().getStateLotteryGames();
            List<LotteryGame> lotteryGames = new ArrayList<>();
            for (LotteryGame lotteryGame : stateLotteryGames) {
                LotteryGame game = lotteryGame;
                if (idx++ == 0) {
                    lotteryGames.add(game);
                } else {
                    game.setLotteryDraws(new ArrayList<>());
                    lotteryGames.add(game);
                }
            }
            stateGamesResponse = new StateGamesResponse();
            stateGamesResponse.setLotteryGames(lotteryGames);
        }
        return Optional.of(stateGamesResponse);
    }
}
