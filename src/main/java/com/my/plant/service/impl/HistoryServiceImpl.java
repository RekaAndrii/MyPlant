package com.my.plant.service.impl;

import com.my.plant.model.HistoryItem;
import com.my.plant.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Override
    public int countDistinctBlocksOnDate(String username, LocalDate date) {
        // History time is stored unshifted, while lastExecution uses now().minusHours(3)
        LocalDateTime from = date.atStartOfDay().plusHours(3);
        LocalDateTime to = from.plusDays(1);
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("userName").is(username),
                Criteria.where("time").gte(from).lt(to)
        ));
        List<HistoryItem> items = mongoTemplate.find(query, HistoryItem.class);
        Set<String> blockNames = new HashSet<>();
        for (HistoryItem item : items) {
            blockNames.add(item.getBlockName());
        }
        return blockNames.size();
    }

    @Override
    public void renameBlock(String oldName, String newName, String userName) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("userName").is(userName),
                Criteria.where("blockName").is(oldName)
        ));
        mongoTemplate.updateMulti(query, Update.update("blockName", newName), HistoryItem.class);
    }
}
