package com.my.plant.service.impl;

import com.my.plant.model.Goal;
import com.my.plant.service.GoalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by User on 31.07.2026.
 */
@Service
public class GoalServiceImpl implements GoalService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<Goal> getAll(String userName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userName").is(userName));
        query.with(Sort.by(Sort.Direction.ASC, "order", "createdDate"));
        return mongoTemplate.find(query, Goal.class);
    }

    @Override
    public Goal findById(String id, String userName) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("_id").is(id),
                Criteria.where("userName").is(userName)
        ));
        return mongoTemplate.findOne(query, Goal.class);
    }

    @Override
    public void save(Goal goal) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userName").is(goal.getUserName()));
        query.with(Sort.by(Sort.Direction.DESC, "order"));
        query.limit(1);
        List<Goal> existing = mongoTemplate.find(query, Goal.class);
        int nextOrder = existing.isEmpty() ? 0 : existing.get(0).getOrder() + 1;
        goal.setOrder(nextOrder);
        mongoTemplate.save(goal);
    }

    @Override
    public void update(String id, String newName, String userName) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("_id").is(id),
                Criteria.where("userName").is(userName)
        ));
        mongoTemplate.updateFirst(query, Update.update("name", newName), Goal.class);
    }

    @Override
    public void markDone(String id, boolean done, String userName) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("_id").is(id),
                Criteria.where("userName").is(userName)
        ));
        mongoTemplate.updateFirst(query, Update.update("done", done), Goal.class);
    }

    @Override
    public void delete(String id, String userName) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("_id").is(id),
                Criteria.where("userName").is(userName)
        ));
        mongoTemplate.remove(query, Goal.class);
    }

    @Override
    public void reorder(List<String> goalIds, String userName) {
        int order = 0;
        for (String id : goalIds) {
            Query query = new Query();
            query.addCriteria(new Criteria().andOperator(
                    Criteria.where("_id").is(id),
                    Criteria.where("userName").is(userName)
            ));
            mongoTemplate.updateFirst(query, Update.update("order", order++), Goal.class);
        }
    }
}
