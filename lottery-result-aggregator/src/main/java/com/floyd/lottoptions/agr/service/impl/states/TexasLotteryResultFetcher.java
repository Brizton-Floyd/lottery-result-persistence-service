package com.floyd.lottoptions.agr.service.impl.states;

import com.floyd.lottoptions.agr.config.LotteryUrlConfig;
import com.floyd.lottoptions.agr.service.DataFetcher;
import com.floyd.lottoptions.agr.service.impl.LotteryResultPollingService;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class TexasLotteryResultFetcher implements DataFetcher {
    protected static final Logger log = LoggerFactory.getLogger(LotteryResultPollingService.class);

    @Override
    public void getData(String name, List<LotteryUrlConfig.GameInfo> gameInfo) throws Exception{
        log.info("Updating Results Lotto Games for: " + name);

        for (LotteryUrlConfig.GameInfo info : gameInfo) {
            InputStream input = new URL(info.getUrl()).openStream();
            Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            CSVReader csvReader = new CSVReader(reader);

            List<String[]> r = csvReader.readAll();
            r.forEach(x -> System.out.println(Arrays.toString(x)));
        }
    }
}
