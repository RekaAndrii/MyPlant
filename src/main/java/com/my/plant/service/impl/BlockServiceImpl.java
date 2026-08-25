package com.my.plant.service.impl;

import com.my.plant.model.Block;
import com.my.plant.service.BlockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by User on 03.06.2017.
 */
@Service
public class BlockServiceImpl implements BlockService{

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<Block> getAllBlocks(String username) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("userName").is(username),
                Criteria.where("completed").ne(true)
        ));
        return mongoTemplate.find(query, Block.class);
    }


    @Override
    public Block findByName(String name, String username) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(Criteria.where("name").is(name),Criteria.where("userName").is(username)));
        return mongoTemplate.findOne(query, Block.class);
    }

    @Override
    public void save(Block block) {
        mongoTemplate.save(block);
    }

    @Override
    public void remove(String name, String username) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(Criteria.where("name").is(name),Criteria.where("userName").is(username)));
        mongoTemplate.remove(query, Block.class);
    }

    @Override
    public void update(String oldName, Block updated, String userName) {
        Block existing = findByName(oldName, userName);
        if (existing == null) {
            return;
        }

        existing.setName(updated.getName());
        existing.setScheduledDays(updated.getScheduledDays());

        boolean wasChallenge = existing.isChallenge();
        boolean nowChallenge = updated.isChallenge();

        if (nowChallenge) {
            Integer newTarget = updated.getTargetExecutions();
            if (newTarget == null || newTarget < 1) {
                newTarget = 1;
            }
            if (wasChallenge) {
                Integer oldTarget = existing.getTargetExecutions();
                Integer oldRemaining = existing.getRemainingExecutions();
                if (oldTarget == null) oldTarget = 0;
                if (oldRemaining == null) oldRemaining = 0;
                int newRemaining = newTarget - (oldTarget - oldRemaining);
                if (newRemaining < 0) newRemaining = 0;
                existing.setRemainingExecutions(newRemaining);
            } else {
                existing.setRemainingExecutions(newTarget);
            }
            existing.setChallenge(true);
            existing.setTargetExecutions(newTarget);
        } else {
            existing.setChallenge(false);
            existing.setTargetExecutions(null);
            existing.setRemainingExecutions(null);
        }

        mongoTemplate.save(existing);
    }

    @Override
    public void updateDisabled(String name, boolean disabled, String userName) {
        Block existing = findByName(name, userName);
        if (existing == null) {
            return;
        }
        existing.setDisabled(disabled);
        mongoTemplate.save(existing);
    }
}
