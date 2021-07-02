package com.floyd.lottoptions.service.impl;

import com.floyd.lottoptions.service.DataService;
import com.floyd.persistence.model.LotteryGame;
import com.floyd.persistence.model.request.StateGameAnalysisRequest;
import com.floyd.persistence.model.response.StateGamesResponse;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

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
}