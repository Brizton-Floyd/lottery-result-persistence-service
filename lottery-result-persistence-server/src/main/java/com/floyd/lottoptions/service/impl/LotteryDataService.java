package com.floyd.lottoptions.service.impl;

import com.floyd.lottoptions.service.DataService;
import com.floyd.persistence.model.LotteryGame;
import com.floyd.persistence.model.LotteryState;
import com.floyd.persistence.model.request.StateGameAnalysisRequest;
import com.floyd.persistence.model.response.AllStateLottoGameResponse;
import com.floyd.persistence.model.response.StateGamesResponse;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LotteryDataService implements DataService {

    private static final Logger log = LoggerFactory.getLogger(LotteryDataService.class);


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
    public List<LotteryState> fetchStates() {
        List<String> directoryNames = getNames("tmp/");

        List<LotteryState> lotteryStates = new ArrayList<>();
        for (String directoryName : directoryNames) {
            LotteryState lotteryState = new LotteryState();
            lotteryState.setStateRegion(directoryName);
            lotteryStates.add(lotteryState);
        }
        return lotteryStates;
    }

    private static List<String> getNames(String directoryName) {
        List<String> directoryNames = new ArrayList<>();
        Path mainDir = Paths.get(directoryName);

        try (Stream<Path> paths = Files.walk(mainDir, 1)) {
            directoryNames = paths
                    .filter(Files::isDirectory)
                    .filter(path -> !path.equals(Paths.get(directoryName))) // Exclude the main directory itself
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return directoryNames;
    }

    @Override
    public List<LotteryGame> fetchStateGames(String stateName) {
//        List<String> games = getNames();

        // Create a File object for the specified directory
        File directory = new File("tmp/" + stateName.toUpperCase() + "/");

        // Create a FilenameFilter to filter .ser files
        FilenameFilter serFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".ser");
            }
        };

        // Get all .ser files in the directory
        File[] serFiles = directory.listFiles(serFilter);
        List<LotteryGame> lotteryGames = new ArrayList<>();

        // Check if the directory is empty or no .ser files found
        if (serFiles == null || serFiles.length == 0) {
            System.out.println("No .ser files found in the directory.");
        } else {
            // Print the names of the .ser files without the .ser extension
            for (File file : serFiles) {
                String fileName = file.getName();
                String nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
                LotteryGame lotteryGame = new LotteryGame();
                lotteryGame.setFullName(nameWithoutExtension);
                lotteryGames.add(lotteryGame);
                System.out.println(nameWithoutExtension);
            }
        }

        return lotteryGames;
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
                        // FR-020-A: copy drawPositionCount from the deserialized .ser game.
                        // This field is set by the aggregator processors before serialization and is the
                        // authoritative source for both the v1 (/all/state-games) and v2 (/all/v2/state-games)
                        // catalog paths, both of which call this method via populate(). (FR-020-B)
                        if (game.getDrawPositionCount() != null) {
                            lotteryGame.setDrawPositionCount(game.getDrawPositionCount());
                        } else {
                            log.warn("drawPositionCount is null for game '{}' in state '{}' — " +
                                     ".ser file may be stale and require re-aggregation.",
                                     game.getFullName(), currentState);
                        }
                        lotteryGame.setLotteryDraws(null);

                        lotteryGameList.add(lotteryGame);
                    }
                   // System.out.println(file.getName());
                }
            }
        }
    }
}