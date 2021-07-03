package com.floyd.lottoptions.agr.service.impl.states;

import com.floyd.lottoptions.agr.config.LotteryUrlConfig;
import com.floyd.lottoptions.agr.service.DataFetcher;
import com.floyd.lottoptions.agr.service.FileType;
import com.floyd.lottoptions.agr.service.impl.LotteryResultPollingService;
import com.floyd.persistence.model.LotteryDraw;
import com.floyd.persistence.model.LotteryGame;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TexasLotteryResultProcessor extends FileType implements DataFetcher {
    protected static final Logger log = LoggerFactory.getLogger(LotteryResultPollingService.class);
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    @Override
    public void getData(String stateName, List<LotteryUrlConfig.GameInfo> gameInfo) throws Exception{
        log.info("Updating Results Lotto Games for: " + stateName);

        for (LotteryUrlConfig.GameInfo info : gameInfo) {
            InputStream input = new URL(info.getUrl()).openStream();
            Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            CSVReader csvReader = new CSVReader(reader);

            List<String[]> drawResultsInCsvFormat = csvReader.readAll();
            if (drawResultsInCsvFormat.size() > 2000) {
                // This will help ensure the serialization of only the past 2000 lottery draw results
                drawResultsInCsvFormat = drawResultsInCsvFormat.subList(drawResultsInCsvFormat.size() - 2000,
                        drawResultsInCsvFormat.size());
            }

            // Begin the serialization process
            processDrawResultsForGivenGame(info.getName(), stateName, drawResultsInCsvFormat);
        }
    }

    private void processDrawResultsForGivenGame(String name, String stateName, List<String[]> drawResultsInCsvFormat) {
        switch (name) {
            case "Lotto Texas":
            case "Cash Five":
                serializeDrawResults(name, drawResultsInCsvFormat, stateName, false, false);
                break;
            case "Texas Two Step":
                serializeDrawResults(name, drawResultsInCsvFormat, stateName, true, false);
                break;
            case "Pick 3 Morning":
            case "Pick 3 Day":
            case "Pick 3 Night":
            case "Pick 3 Evening":
            case "Daily 4 Morning":
            case "Daily 4 Day":
            case "Daily 4 Evening":
            case "Daily 4 Night":
                serializeDrawResults(name, drawResultsInCsvFormat, stateName, false, true);

        }
    }

    private void serializeDrawResults(String gameName,
                                      List<String[]> drawResultsInCsvFormat,
                                      String stateName,
                                      boolean includeBonus,
                                      boolean isFireBallIncluded) {
        log.info("Beginning serialization for " + stateName + ": " + gameName);
        LotteryGame lotteryGame = new LotteryGame();
        lotteryGame.setFullName(gameName);
        for (String[] data : drawResultsInCsvFormat) {
            final String date = data[3] + "-" + format(data[1]) + "-" + format(data[2]);
            final LocalDate drawDate = LocalDate.parse(date);

            LotteryDraw lotteryDraw = new LotteryDraw();
            int bound = 0;
            if (includeBonus) {
                int bonusNumber = Integer.parseInt(data[data.length - 1]);
                lotteryDraw.setBonusNumber(bonusNumber);
                bound = -1;
            }

            if (isFireBallIncluded) {
                bound = - 2;
            }

            int counter = 0;
            lotteryDraw.setDrawDate(drawDate);
            while (counter < 4) counter++; // increment counter until draw result begins
            try {
                while (counter < (data.length + bound)) {
                    lotteryDraw.getDrawResults().add(Integer.parseInt(data[counter++]));
                }
                Collections.sort(lotteryDraw.getDrawResults());
                lotteryGame.getLotteryDraws().add(lotteryDraw);

            } catch (NumberFormatException e) {
                log.error("Error occurred while attempting to add winning number for lotto game: " + stateName + ": " + gameName);
            }
        }

        int size = lotteryGame.getLotteryDraws().size() - 1;
        lotteryGame.setDateLastUpdated(lotteryGame.getLotteryDraws().get(size).getDrawDate());
        lotteryGame.setDrawPositionCount(lotteryGame.getLotteryDraws().get(0).getDrawResults().size());
        lotteryGame.setDrawHistoryCount(lotteryGame.getLotteryDraws().size());

        try {
            File file = new File("tmp/" + stateName.toUpperCase());
            boolean mkdir = file.mkdirs();
            serializeData(file.getPath() + "/" + gameName + ".ser", lotteryGame);
            log.info("Successfully serialized draw results for " + stateName + ": " + gameName);
        } catch (IOException e) {
            log.error("Error happened during serialization of: " + gameName, e);
        }
    }

    private void serializeData(String directory, Object obj) throws IOException {
        //Saving of object in a file
        FileOutputStream file = new FileOutputStream(directory, false);
        ObjectOutputStream out = new ObjectOutputStream(file);

        // Method for serialization of object
        out.writeObject(obj);

        out.close();
        file.close();
    }

    private String format(String elementInDateString) {
        return (elementInDateString.length() == 1) ? ("0" + elementInDateString) : elementInDateString;
    }
}
