package com.floyd.lottoptions.agr.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.floyd.lottoptions.agr.config.LotteryRegionConfig;
import com.floyd.lottoptions.agr.config.MongoConfig;
import com.floyd.lottoptions.agr.repository.LotteryStateRepository;
import com.floyd.lottoptions.agr.service.PollingService;
import model.LotteryDraw;
import model.LotteryGame;
import model.LotteryState;
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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
public class LotteryResultPollingService implements PollingService {

    private static final Logger log = LoggerFactory.getLogger(LotteryResultPollingService.class);
    private final LotteryRegionConfig lotteryRegionConfig;
    private final LotteryStateRepository lotteryStateRepository;
    private final WebClient webClient;
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
                                       WebClient webClient) {
        this.lotteryRegionConfig = lotteryRegionConfig;
        this.lotteryStateRepository = lotteryStateRepository;
        this.mongoConfig = mongoConfig;
        this.webClient = webClient;
    }

    @Override
    public void pollForUpdatesToDrawResults() throws Exception {
        log.info("Updating state lotteries");

        for (String region : lotteryRegionConfig.getRegions()) {
            LotteryState lotteryState = mongoConfig.mongoTemplate()
                    .findOne(Query.query(Criteria.where("stateRegion").is(region)), LotteryState.class);
            if (lotteryState != null) {
                List<LotteryGame> nonCardLotteryGames = getNonCardLotteryGames(lotteryState);
                for (LotteryGame lotteryGame : nonCardLotteryGames) {
                    if (lotteryGame.getLotteryDraws() == null) {
                        lotteryGame.setLotteryDraws(new ArrayList<>());
                    }

                    int gamesToQueryFor = (lotteryGame.getLotteryDraws().size() > 0) ?
                            (int) getGameCountToQueryFor(lotteryGame) : 1000;

                    if (gamesToQueryFor > 0) {
                        String queryString = populateQueryString(lotteryGame, gamesToQueryFor, region);
                        String data = webClient.post()
                                .uri(drawResultUri)
                                .contentType(MediaType.APPLICATION_JSON)
                                .headers(this::populateHeaders)
                                .body(BodyInserters.fromValue(queryString))
                                .retrieve()
                                .bodyToMono(String.class)
                                .block();
                        updateStateLotteryGame(data, lotteryGame, lotteryState);
                    } else {
                        log.info(String.format("No game updates needed for %s", lotteryGame.getFullName()));
                    }
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
     *
     * @param lotteryState
     * @return
     */
    private List<LotteryGame> getNonCardLotteryGames(LotteryState lotteryState) {
        return lotteryState.getStateLotteryGames().stream()
                .filter(game -> !game.getType().contains("CARD"))
                .collect(Collectors.toList());
    }

    /**
     *
     * @param lotteryGame
     * @return
     */
    private long getGameCountToQueryFor(LotteryGame lotteryGame) {
        if (lotteryGame.getLotteryDraws().get(0).getResults().getAsOfDate() == null) {
            return -1;
        }
        // Grab the most recent draw date from the database
        String mostRecentDrawDate = lotteryGame.getLotteryDraws().get(0).getResults().getAsOfDate();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime mostRecentDrawOffsetDate = OffsetDateTime.parse(mostRecentDrawDate);
        //Period duration = Period.between(now.toLocalDate(), mostRecentDrawOffsetDate.toLocalDate());
        // Calculate the difference between current date now and previous draw date
        Duration difference = Duration.between(mostRecentDrawOffsetDate, now);
        return difference.toDays();
    }

    /**
     * @param data
     * @param lotteryGame
     * @param lotteryState
     */
    private void updateStateLotteryGame(String data, LotteryGame lotteryGame, LotteryState lotteryState) {
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            JsonNode node = mapper.readTree(data);
            JsonNode gameInformationNode = node.at("/data/game");
            String lastGameDate = gameInformationNode.get("lastGameDate").textValue();
            String nextGameDate = gameInformationNode.get("nextGameDate").textValue();

            JsonNode drawNode = node.at("/data/game/draws");
            LotteryDraw[] lotteryDraws = mapper.treeToValue(drawNode, LotteryDraw[].class);

            lotteryGame.setNextGameDate(nextGameDate);
            lotteryGame.setLastGameDate(lastGameDate);

            List<LotteryDraw> currentDrawData = lotteryGame.getLotteryDraws();
            
            // This is to ensure the data in the database never grows post 2000 drawings
            if (currentDrawData.size() > 2000) {
                currentDrawData.remove(currentDrawData.size() - 1);
            }

            if (currentDrawData.size() > 1) {
                List<LotteryDraw> drawsToInsert =
                        getGamesNeedingInsert(lotteryGame.getLotteryDraws().get(0).getResults().getAsOfDate(), lotteryDraws);
                if (drawsToInsert.size() >= 1) {
                    String postFix = (drawsToInsert.size() == 1) ? "Draw" : "Draws";
                    log.info(String.format("Inserting %d %s for lotto game: %s", drawsToInsert.size(), postFix,
                            lotteryGame.getFullName()));
                    for (int i = drawsToInsert.size() - 1; i >= 0; i--) {
                        currentDrawData.add(0, drawsToInsert.get(i));
                    }
                } else {
                    log.info(String.format("No recent drawings to insert for %s", lotteryGame.getFullName()));
                    return;
                }
            } else {
                // If there happens to be a brand new game being added to state grab up to a thousand of the most recent
                // drawings
                log.info(String.format("Inserting last 1000 draws for %s", lotteryGame.getFullName()));
                currentDrawData.addAll(Arrays.asList(lotteryDraws));
            }
            // Update and save game data for the state
            updateAndSaveLotteryStateData(lotteryGame, lotteryState, currentDrawData);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     *
     * @param lotteryGame
     * @param lotteryState
     * @param currentDrawData
     */
    private void updateAndSaveLotteryStateData(LotteryGame lotteryGame, LotteryState lotteryState, List<LotteryDraw> currentDrawData) {
        log.info(format("Performing game update for %s in region %s", lotteryGame.getFullName(), lotteryState.getStateRegion()));
        final Optional<LotteryGame> gameToUpdate =
                lotteryState.getStateLotteryGames()
                        .stream()
                        .filter(g -> g.getFullName().equals(lotteryGame.getFullName()))
                        .findFirst();
        gameToUpdate.ifPresent(game -> game.setLotteryDraws(currentDrawData));
        gameToUpdate.ifPresent(game -> game.setDrawHistoryCount(String.valueOf(currentDrawData.size())));
        lotteryStateRepository.save(lotteryState);
        log.info(String.format("Successfully saved data for: %s %s", lotteryState.getStateRegion(), lotteryGame.getFullName()));
    }

    /**
     *
     * @param mostRecentDateInDb
     * @param lotteryDraws
     * @return
     */
    private List<LotteryDraw> getGamesNeedingInsert(String mostRecentDateInDb,
                                                    LotteryDraw[] lotteryDraws) {
        LinkedList<LotteryDraw> gamesNeedingInsert = new LinkedList<>();
        for (final LotteryDraw lotteryDraw : lotteryDraws) {
            if (lotteryDraw.getResults() == null || lotteryDraw.getResults().getAsOfDate() == null) {
                continue;
            }
            if (!lotteryDraw.getResults().getAsOfDate().equals(mostRecentDateInDb)) {
                gamesNeedingInsert.addLast(lotteryDraw);
            } else {
                break;
            }
        }

        return gamesNeedingInsert;
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
