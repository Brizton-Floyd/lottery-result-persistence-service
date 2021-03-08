package model.response;

import lombok.Data;
import model.LotteryGame;

import java.util.List;

@Data
public class StateGamesResponse {
    List<LotteryGame> lotteryGames;
}
