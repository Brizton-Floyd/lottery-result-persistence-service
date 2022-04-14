package com.floyd.lottoptions.agr.persistence;

import com.floyd.persistence.model.LotteryGame;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class LotteryGameSerializer {
    public boolean serialize(LotteryGame lotteryGame) throws IOException {
        final String gameName = lotteryGame.getFullName();
        File file = new File("tmp/" + lotteryGame.getStateGameBelongsTo().toUpperCase());
        String path = file.getPath() + "/" + gameName + ".ser";
        boolean mkdir = file.mkdirs();

        try (FileOutputStream outputStream = new FileOutputStream(path, false);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(lotteryGame);
        }

        return true;
    }
}
