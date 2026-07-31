package com.my.plant.service.impl;

import com.my.plant.model.Achievement;
import com.my.plant.service.AchievementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by User on 24.07.2026.
 */
@Service
public class AchievementServiceImpl implements AchievementService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void save(Achievement achievement) {
        mongoTemplate.save(achievement);
    }

    @Override
    public List<Achievement> getAll(String userName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userName").is(userName));
        query.with(Sort.by(Sort.Direction.DESC, "achievedDate"));
        return mongoTemplate.find(query, Achievement.class);
    }

    @Override
    public List<Achievement> getUnlinked(String userName) {
        Query query = new Query();
        query.addCriteria(new org.springframework.data.mongodb.core.query.Criteria().andOperator(
                Criteria.where("userName").is(userName),
                new org.springframework.data.mongodb.core.query.Criteria().orOperator(
                        Criteria.where("goalStepId").exists(false),
                        Criteria.where("goalStepId").is(null)
                )
        ));
        query.with(Sort.by(Sort.Direction.DESC, "achievedDate"));
        return mongoTemplate.find(query, Achievement.class);
    }
}
