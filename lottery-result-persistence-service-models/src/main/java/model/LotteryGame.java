package model;

import lombok.Data;

import java.util.List;

@Data
public class LotteryGame {
    private String id;
    private String fullName;
    private String shortName;
    private String type;
    private String abbrev;
    private String description;
    private String createdAt;
    private String updatedAt;
    private String officialUrl;
    private String nextGameDate;
    private String lastGameDate;
    List<LotteryDraw> lotteryDraws;
}
