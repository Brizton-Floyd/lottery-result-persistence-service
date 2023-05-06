package com.floyd.lottoptions.service.impl;

import com.floyd.lottoptions.service.DataService;
import com.floyd.persistence.model.LotteryGame;
import com.floyd.persistence.model.LotteryState;
import com.floyd.persistence.model.request.StateGameAnalysisRequest;
import com.floyd.persistence.model.response.AllStateLottoGameResponse;
import com.floyd.persistence.model.response.StateGamesResponse;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class LotteryDataService implements DataService {


    @Override
    public Optional<StateGamesResponse> getStateData(StateGameAnalysisRequest stateGameAnalysisRequest) throws Exception {
        final String gameName = stateGameAnalysisRequest.getGameName();
        final String stateName = stateGameAnalysisRequest.getStateName();
        StateGamesResponse stateGamesResponse = new StateGamesResponse();

        File root = new File("tmp/" + stateName.toUpperCase());
        String fileName = gameName + ".ser";
        try {
            Collection<File> files = FileUtils.listFiles(root, null, true);

            for (Iterator<File> iterator = files.iterator(); iterator.hasNext();) {
                File file = iterator.next();
                if (file.getName().equals(fileName)) {

                    FileInputStream fileIn = new FileInputStream(root + "/" + fileName);
                    ObjectInputStream in = new ObjectInputStream(fileIn);
                    LotteryGame lotteryGame = null;
                    try {
                        lotteryGame = (LotteryGame) in.readObject();
                        stateGamesResponse.setLotteryGame(lotteryGame);
                    } catch (ClassNotFoundException ee) {
                        ee.printStackTrace();
                    }
                    in.close();
                    fileIn.close();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.of(stateGamesResponse);
    }

    @Override
    public Optional<AllStateLottoGameResponse> getAllStateLotteryGames() throws Exception {
        // Gather all directories within given directory
            // The parent directory will be the state name
                // all child directories will be the game names
        Map<String, List<LotteryGame>> map = new HashMap<>();
        populate(map);

        // for all lottery games names in ascending order
        map.values().forEach(lst -> lst.sort(Comparator.comparing(LotteryGame::getFullName)));

        AllStateLottoGameResponse allStateLottoGameResponse = new AllStateLottoGameResponse();
        allStateLottoGameResponse.setAllStateLotteryGames(map);
        return Optional.of(allStateLottoGameResponse);
    }

    @Override
    public Optional<AllStateLottoGameResponse> getAllStateLotteryGamesV2() throws Exception {
        AllStateLottoGameResponse allStateLottoGameResponse = new AllStateLottoGameResponse();

        Map<String, List<LotteryGame>> map = new HashMap<>();
        populate(map);

        // for all lottery games names in ascending order
        map.values().forEach(lst -> lst.sort(Comparator.comparing(LotteryGame::getFullName)));

        List<LotteryState> lotteryStates = new ArrayList<>();
        map.forEach((state, games) -> {
            LotteryState lotteryState = new LotteryState();
            lotteryState.setStateRegion(state);
            lotteryState.setStateLotteryGames(games);

            lotteryStates.add(lotteryState);
        });

        allStateLottoGameResponse.setLotteryStateGames(lotteryStates);

        return Optional.of(allStateLottoGameResponse);
    }

    @Override
    public Optional<List<String>> getAllStateLotteryGames(String state) throws Exception {
        List<String> games = new ArrayList<>();
        File directory = new File("tmp/" + state + "/");
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    int index = file.getName().indexOf('.');
                    games.add(file.getName().substring(0, index));
                }
            }
        }
        return Optional.of(games);
    }

    private void populate(Map<String, List<LotteryGame>> map) throws Exception{
        File directory = new File("tmp/");
        listFilesForFolder(directory, map, "");
    }

    private void listFilesForFolder(final File folder, Map<String, List<LotteryGame>> map, String state) throws Exception {
        String currentState = state;
        if (folder.exists()) {
            File[] fList = folder.listFiles();
            if (fList != null) {
                for (File file : fList) {
                    if (file.isDirectory()) {
                        currentState = file.getName().charAt(0) + file.getName().substring(1).toLowerCase();
                        map.put(currentState, new ArrayList<>());
                        listFilesForFolder(file, map, currentState);
                    } else {

                        String lotteryGameName = file.getName().split(Pattern.quote("."))[0];
                        StateGameAnalysisRequest request = new StateGameAnalysisRequest();
                        request.setStateName(currentState);
                        request.setGameName(lotteryGameName);

                        Optional<StateGamesResponse> stateData = getStateData(request);
                        StateGamesResponse stateGamesResponse = stateData.get();
                        LotteryGame game = stateGamesResponse.getLotteryGame();

                        List<LotteryGame> lotteryGameList = map.get(currentState);
                        LotteryGame lotteryGame = new LotteryGame();
                        lotteryGame.setFullName(game.getFullName());
                        lotteryGame.setStateGameBelongsTo(currentState);
                        lotteryGame.setMinNumber(game.getMinNumber());
                        lotteryGame.setMaxNumber(game.getMaxNumber());
                        lotteryGame.setLotteryDraws(null);

                        lotteryGameList.add(lotteryGame);
                    }
                   // System.out.println(file.getName());
                }
            }
        }
    }
}