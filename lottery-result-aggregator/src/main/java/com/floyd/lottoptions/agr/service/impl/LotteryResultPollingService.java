package com.floyd.lottoptions.agr.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.floyd.lottoptions.agr.config.LotteryRegionConfig;
import com.floyd.lottoptions.agr.config.MongoConfig;
import com.floyd.lottoptions.agr.model.LotteryDraw;
import com.floyd.lottoptions.agr.model.LotteryGame;
import com.floyd.lottoptions.agr.model.LotteryState;
import com.floyd.lottoptions.agr.repository.LotteryStateRepository;
import com.floyd.lottoptions.agr.service.PollingService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
public class LotteryResultPollingService implements PollingService {

    private static final Logger log = LoggerFactory.getLogger(LotteryResultPollingService.class);
    private final LotteryRegionConfig lotteryRegionConfig;
    private final LotteryStateRepository lotteryStateRepository;
    private final WebClient webClient;
    private final RateLimiter rateLimiter;
    private final MongoConfig mongoConfig;
    @Value("${state.games.api.host}")
    private String host;
    @Value("${state.games.api.uri}")
    private String uri;
    @Value("${draw.result.uri}")
    private String drawResultUri;

    public LotteryResultPollingService(LotteryRegionConfig lotteryRegionConfig,
                                       LotteryStateRepository lotteryStateRepository,
                                       MongoConfig mongoConfig,
                                       WebClient webClient,
                                       RateLimiter rateLimiter) {
        this.lotteryRegionConfig = lotteryRegionConfig;
        this.lotteryStateRepository = lotteryStateRepository;
        this.mongoConfig = mongoConfig;
        this.webClient = webClient;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void pollForUpdatesToDrawResults() throws Exception {
        log.info("Updating state lotteries");

        for (String region : lotteryRegionConfig.getRegions()) {
            LotteryState lotteryState = mongoConfig.mongoTemplate()
                    .findOne(Query.query(Criteria.where("stateRegion").is(region)), LotteryState.class);
            if (lotteryState != null) {
                List<LotteryGame> nonCardLotteryGames =
                        lotteryState.getStateLotteryGames().stream()
                        .filter(game -> !game.getType().contains("CARD"))
                        .collect(Collectors.toList());
                for (LotteryGame lotteryGame : nonCardLotteryGames) {
                    if (lotteryGame.getLotteryDraws() == null) {
                        lotteryGame.setLotteryDraws(new ArrayList<>());
                    }
                    
                    int last = (lotteryGame.getLotteryDraws().size() > 1) ? 1 : 1000;
                    String queryString = populateQueryString(lotteryGame, last, region);
                    String data = webClient.post()
                            .uri(drawResultUri)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(this::populateHeaders)
                            .body(BodyInserters.fromValue(queryString))
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();
                    updateStateLotteryGame(data, lotteryGame, lotteryState);
                }
            }
        }
        log.info("Concluded updating of state lotteries");
    }
    
    @Override
    public void pollForUpdatesToStateGames() throws Exception {
        for (String region : lotteryRegionConfig.getRegions()) {
            String fullUri = format("%s%s%s", host, uri, region);
            log.info(format("Gathering lottery game information for region: %s", region));
            webClient.get().uri(new URI(fullUri))
                    .retrieve().bodyToMono(String.class)
                    .subscribe(json -> constructLotteryStateObjectForPersistence(json, region));
        }
    }

    /**
     *  @param data
     * @param lotteryGame
     * @param lotteryState
     */
    private void updateStateLotteryGame(String data, LotteryGame lotteryGame, LotteryState lotteryState) {
        log.info(format("Performing game update for %s in region %s",lotteryGame.getFullName(), lotteryState.getStateRegion()));
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            JsonNode node = mapper.readTree(data);
            JsonNode drawNode = node.at("/data/game/draws");
            LotteryDraw[] lotteryDraws = mapper.treeToValue(drawNode, LotteryDraw[].class);

            List<LotteryDraw> currentDrawData = lotteryGame.getLotteryDraws();
            
            if (currentDrawData.size() > 1) {
                // TODO: add only most recent element in the lotteryDrawsArray
                String mostRecentDateInDb = currentDrawData.get(0).getResults().getAsOfDate();
                String mostRecentDateFromApi = lotteryDraws[0].getResults().getAsOfDate();
                if (mostRecentDateInDb.equals(mostRecentDateFromApi)) {
                    log.info(String.format("There is no new draw information for %s", lotteryGame.getFullName()));
                    return;
                } else {
                    log.info(String.format("Inserting new draw for %s", lotteryGame.getFullName()));
                    currentDrawData.set(0, lotteryDraws[0]);
                }
                
            }else {
                currentDrawData.addAll(Arrays.asList(lotteryDraws));
            }
            final Optional<LotteryGame> gameToUpdate =
                    lotteryState.getStateLotteryGames()
                            .stream()
                            .filter(g -> g.getFullName().equals(lotteryGame.getFullName()))
                    .findFirst();
            gameToUpdate.ifPresent(game -> game.setLotteryDraws(currentDrawData));
            lotteryStateRepository.save(lotteryState);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    
    /**
     * @param jsonResponse
     * @param region
     */
    private void constructLotteryStateObjectForPersistence(String jsonResponse, String region) {
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            LotteryGame[] lotteryGames = mapper.readValue(jsonResponse, LotteryGame[].class);
            LotteryState lotteryState = new LotteryState();
            lotteryState.setStateRegion(region);
            lotteryState.setStateLotteryGames(Arrays.asList(lotteryGames));

            LotteryState lotteryState1 = mongoConfig.mongoTemplate()
                    .findOne(Query.query(Criteria.where("stateRegion").is(region)), LotteryState.class);

            if (lotteryState1 == null) {
                log.info(format("Inserting new lottery game information for region: %s", region));
                lotteryStateRepository.save(lotteryState);
            } else {
                log.info(format("Performing updates to lottery games in region: %s", region));
                lotteryState1.setStateLotteryGames(Arrays.asList(lotteryGames));
                lotteryStateRepository.save(lotteryState1);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private String populateQueryString(LotteryGame lotteryGame, int last, String region) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return format(getQuery(), lotteryGame.getId(), lotteryGame.getCreatedAt(), now.toString(), last, region);
    }

    private String getQuery() {
        return "{\r\n    \"operationName\": \"ResultsHistoryView\",\r\n    \"query\": \"query ResultsHistoryView($startingAt: GraphQLDateTime!, $endingAt: GraphQLDateTime!, $selectedId: ID!) {\\n  game(id: $selectedId) {\\n    ...GameType\\n    draws(startingAt: $startingAt, endingAt: $endingAt, last: %4$s, next: 1) {\\n      ...DrawType\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\\nfragment GameType on Game {\\n  _id: id\\n  enabled\\n  fullName\\n  lastGameDate\\n  nextGameDate\\n  regions\\n  shortName\\n  type\\n  resultFormat {\\n    ...ResultFormatType\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment ResultFormatType on ResultFormat {\\n  count\\n  resultTypes {\\n    ...ResultFormatTypeType\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment ResultFormatTypeType on ResultFormatType {\\n  category\\n  type\\n  __typename\\n}\\n\\nfragment DrawType on Draw {\\n  _id: id\\n  prizes {\\n    ...DrawPrizeType\\n    __typename\\n  }\\n  results {\\n    ...ResultType\\n    __typename\\n  }\\n  resultsAnnouncedAt\\n  __typename\\n}\\n\\nfragment DrawPrizeType on DrawPrize {\\n  asOfDate\\n  values {\\n    ...PrizeValueType\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment PrizeValueType on PrizeValue {\\n  value\\n  type\\n  name\\n  __typename\\n}\\n\\nfragment ResultType on Result {\\n  asOfDate\\n  values {\\n    ...ResultValueType\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment ResultValueType on ResultValue {\\n  value\\n  type\\n  name\\n  category\\n  __typename\\n}\\n\",\r\n    \"variables\": {\r\n        \"endingAt\": \"%3$s\",\r\n        \"selectedRegion\": \"%5$s\",\r\n        \"startingAt\": \"%2$s\", \r\n        \"selectedId\": \"%1$s\"\r\n    }\r\n}";
    }

    private void populateHeaders(HttpHeaders httpHeaders) {
        httpHeaders.set("Host", "graffy.api.lottery.com");
        httpHeaders.set("Content-Type", "application/json");
        httpHeaders.set("Connection", "keep-alive");
        httpHeaders.set("Accept", "/");
        httpHeaders.set("User-Agent", "LDC/1 CFNetwork/1128.0.1 Darwin/19.6.0");
        httpHeaders.set("Accept-Language", "en-us");
        httpHeaders.set("Accept-Encoding", "gzip, deflate, br");
    }
}
