package com.my.plant.service.impl;

import com.my.plant.model.HistoryItem;
import com.my.plant.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by User on 11.06.2017.
 */
@Service
public class HistoryServiceImpl implements HistoryService{

    @Autowired
    private MongoTemplate mongoTemplate;


    @Override
    public List<HistoryItem> getUserHistory(String username) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userName").is(username));
        return mongoTemplate.find(query, HistoryItem.class);
    }

    @Override
    public void save(HistoryItem item) {
        mongoTemplate.save(item);
    }
}
