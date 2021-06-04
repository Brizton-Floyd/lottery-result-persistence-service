package com.floyd.persistence.model;

import lombok.Data;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.mapping.Document;

@Data
//@Document(collection = "drawresults")
public class LotteryDraw {
   // @Id
    private String id;
    private Prizes prizes;
    private Results results;
}
