package com.floyd.lottoptions.agr.repository;

import com.floyd.lottoptions.agr.model.LotteryState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LotteryStateRepository extends MongoRepository<LotteryState, String> {

}
