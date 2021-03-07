package com.floyd.lottoptions.agr.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "lotterystates")
public class LotteryState {

    @Id
    private String id;
    private String stateRegion;
    private List<LotteryGame> stateLotteryGames;
}
