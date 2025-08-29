package com.floyd.lottoptions.agr.processor;

import com.floyd.lottoptions.agr.config.LotteryUrlConfig;
import com.floyd.lottoptions.agr.documentreaders.FileReader;
import com.floyd.lottoptions.agr.persistence.LotteryGameSerializer;
import com.floyd.lottoptions.agr.polling.LotteryResultPollingService;
import com.floyd.persistence.model.LotteryDraw;
import com.floyd.persistence.model.LotteryGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TexasLotteryHistoryProcessor implements HistoryProcessor {
    protected static final Logger log = LoggerFactory.getLogger(LotteryResultPollingService.class);
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private final FileReader fileReader;
    private final LotteryGameSerializer lotteryGameSerializer;

    public TexasLotteryHistoryProcessor(FileReader fileReader, LotteryGameSerializer lotteryGameSerializer) {
        this.fileReader = fileReader;
        this.lotteryGameSerializer = lotteryGameSerializer;
    }

    @Override
    public void getHistoricalData(String stateName, List<LotteryUrlConfig.GameInfo> gameInfo) throws Exception{
        log.info("Updating Results Lotto Games for: " + stateName);

        for (LotteryUrlConfig.GameInfo info : gameInfo) {
            List<String[]> drawResultsInCsvFormat = this.fileReader.getFileContents(info);
            if (drawResultsInCsvFormat.size() > 8000) {
                // This will help ensure the serialization of only the past 8000 lottery draw results
                drawResultsInCsvFormat = drawResultsInCsvFormat.subList(drawResultsInCsvFormat.size() - 8000,
                        drawResultsInCsvFormat.size());
            }

            // Begin the serialization process
            processDrawResultsForGivenGame(info.getName(), stateName, drawResultsInCsvFormat);
        }
    }

    private void processDrawResultsForGivenGame(String lottoGameName, String stateName, List<String[]> drawResultsInCsvFormat) {
        switch (lottoGameName) {
            case "Lotto Texas":
            case "Cash Five":
            case "All or Nothing Morning":
            case "All or Nothing Day":
            case "All or Nothing Evening":
            case "All or Nothing Night":
                serializeDrawResults(lottoGameName, drawResultsInCsvFormat, stateName, false, false, true);
                break;
            case "Texas Two Step":
            case "Powerball":
            case "Mega Millions":
                serializeDrawResults(lottoGameName, drawResultsInCsvFormat, stateName, true, false, true);
                break;
            case "Pick 3 Morning":
            case "Pick 3 Day":
            case "Pick 3 Night":
            case "Pick 3 Evening":
            case "Daily 4 Morning":
            case "Daily 4 Day":
            case "Daily 4 Evening":
            case "Daily 4 Night":
                serializeDrawResults(lottoGameName, drawResultsInCsvFormat, stateName, false, true, false);
                break;

        }
    }

    private void serializeDrawResults(String gameName,
                                      List<String[]> drawResultsInCsvFormat,
                                      String stateName,
                                      boolean includeBonus,
                                      boolean isFireBallIncluded,
                                      boolean sortResults) {

        log.info("Beginning serialization for " + stateName + ": " + gameName);
        LotteryGame lotteryGame = new LotteryGame();
        lotteryGame.setStateGameBelongsTo(stateName);
        lotteryGame.setFullName(gameName);
        lotteryGame.setLotteryDraws(new ArrayList<>());

        for (String[] data : drawResultsInCsvFormat) {
            LotteryDraw lotteryDraw = new LotteryDraw();
            final LocalDate drawDate = getDrawDate(data);
            lotteryDraw.setDrawDate(drawDate);

            int tail = 0;
            try {
                tail = data.length - 1;
                if (includeBonus) {
                    int bonusNumber = Integer.parseInt(data[data.length - 1]);
                    lotteryDraw.setBonusNumber(bonusNumber);
                    --tail;
                    if (gameName.equals("Powerball") || gameName.equals("Mega Millions")) {
                        --tail;
                    }
                }
            } catch (NumberFormatException e) {
                log.error("Error occurred while attempting to add bonus number for lotto game: " + stateName + ": " + gameName);
            }



            if (isFireBallIncluded) {
                tail -= 2;
            }

            int headPtr = 4;
            try {
                while (headPtr <= tail) {
                    lotteryDraw.getDrawResults().add(Integer.parseInt(data[headPtr++]));
                }

                if (sortResults)
                    Collections.sort(lotteryDraw.getDrawResults());

                lotteryGame.getLotteryDraws().add(lotteryDraw);
                //Collections.sort(lotteryDraw.getDrawResults());
            } catch (NumberFormatException e) {
                log.error("Error occurred while attempting to add winning number for lotto game: " + stateName + ": " + gameName);
            }
        }

        int size = lotteryGame.getLotteryDraws().size() - 1;
        lotteryGame.findMinMaxLottoNumber(stateName);
        lotteryGame.setDateLastUpdated(lotteryGame.getLotteryDraws().get(size).getDrawDate());
        lotteryGame.setDrawPositionCount(lotteryGame.getLotteryDraws().get(0).getDrawResults().size());
        lotteryGame.setDrawHistoryCount(lotteryGame.getLotteryDraws().size());

        try {
            boolean isSerialized = this.lotteryGameSerializer.serialize(lotteryGame);
            log.info("Successfully serialized draw results for " + stateName + ": " + gameName + "= " + isSerialized);
        } catch (IOException e) {
            log.error("Error happened during serialization of: " + gameName, e);
        }
    }

    private LocalDate getDrawDate(String[] data) {
        final String date = data[3] + "-" + format(data[1]) + "-" + format(data[2]);
        final LocalDate drawDate = LocalDate.parse(date);
        return drawDate;
    }

    private String format(String elementInDateString) {
        return (elementInDateString.length() == 1) ? ("0" + elementInDateString) : elementInDateString;
    }
}
