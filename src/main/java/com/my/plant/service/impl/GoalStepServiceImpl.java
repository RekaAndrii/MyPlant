package com.my.plant.service.impl;

import com.my.plant.model.GoalStep;
import com.my.plant.service.GoalStepService;
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
public class GoalStepServiceImpl implements GoalStepService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<GoalStep> getByGoalId(String goalId, String userName) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("goalId").is(goalId),
                Criteria.where("userName").is(userName)
        ));
        query.with(Sort.by(Sort.Direction.ASC, "order"));
        return mongoTemplate.find(query, GoalStep.class);
    }

    @Override
    public void save(GoalStep step) {
        // Determine next order value
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("goalId").is(step.getGoalId()),
                Criteria.where("userName").is(step.getUserName())
        ));
        query.with(Sort.by(Sort.Direction.DESC, "order"));
        query.limit(1);
        List<GoalStep> existing = mongoTemplate.find(query, GoalStep.class);
        int nextOrder = existing.isEmpty() ? 0 : existing.get(0).getOrder() + 1;
        step.setOrder(nextOrder);
        mongoTemplate.save(step);
    }

    @Override
    public void update(String id, String newName, List<String> linkedBlockNames, String userName) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("_id").is(id),
                Criteria.where("userName").is(userName)
        ));
        Update update = new Update();
        update.set("name", newName);
        if (linkedBlockNames != null && !linkedBlockNames.isEmpty()) {
            update.set("linkedBlockNames", linkedBlockNames);
        } else {
            update.unset("linkedBlockNames");
        }
        mongoTemplate.updateFirst(query, update, GoalStep.class);
    }

    @Override
    public void markDone(String id, boolean done, String userName) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("_id").is(id),
                Criteria.where("userName").is(userName)
        ));
        mongoTemplate.updateFirst(query, Update.update("done", done), GoalStep.class);
    }

    @Override
    public void moveUp(String id, String userName) {
        GoalStep step = findStepById(id, userName);
        if (step == null || step.getOrder() == 0) {
            return;
        }
        // Find the step just above (lower order)
        Query prevQuery = new Query();
        prevQuery.addCriteria(new Criteria().andOperator(
                Criteria.where("goalId").is(step.getGoalId()),
                Criteria.where("userName").is(userName),
                Criteria.where("order").lt(step.getOrder())
        ));
        prevQuery.with(Sort.by(Sort.Direction.DESC, "order"));
        prevQuery.limit(1);
        List<GoalStep> prevList = mongoTemplate.find(prevQuery, GoalStep.class);
        if (prevList.isEmpty()) {
            return;
        }
        GoalStep prev = prevList.get(0);
        swapOrders(step, prev, userName);
    }

    @Override
    public void moveDown(String id, String userName) {
        GoalStep step = findStepById(id, userName);
        if (step == null) {
            return;
        }
        // Find the step just below (higher order)
        Query nextQuery = new Query();
        nextQuery.addCriteria(new Criteria().andOperator(
                Criteria.where("goalId").is(step.getGoalId()),
                Criteria.where("userName").is(userName),
                Criteria.where("order").gt(step.getOrder())
        ));
        nextQuery.with(Sort.by(Sort.Direction.ASC, "order"));
        nextQuery.limit(1);
        List<GoalStep> nextList = mongoTemplate.find(nextQuery, GoalStep.class);
        if (nextList.isEmpty()) {
            return;
        }
        GoalStep next = nextList.get(0);
        swapOrders(step, next, userName);
    }

    @Override
    public void delete(String id, String userName) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("_id").is(id),
                Criteria.where("userName").is(userName)
        ));
        mongoTemplate.remove(query, GoalStep.class);
    }

    @Override
    public void deleteByGoalId(String goalId, String userName) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("goalId").is(goalId),
                Criteria.where("userName").is(userName)
        ));
        mongoTemplate.remove(query, GoalStep.class);
    }

    @Override
    public void markDoneByBlockName(String blockName, String userName) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("linkedBlockNames").is(blockName),
                Criteria.where("userName").is(userName),
                Criteria.where("done").is(false)
        ));
        mongoTemplate.updateMulti(query, Update.update("done", true), GoalStep.class);
    }

    private GoalStep findStepById(String id, String userName) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("_id").is(id),
                Criteria.where("userName").is(userName)
        ));
        return mongoTemplate.findOne(query, GoalStep.class);
    }

    private void swapOrders(GoalStep a, GoalStep b, String userName) {
        int orderA = a.getOrder();
        int orderB = b.getOrder();

        Query queryA = new Query();
        queryA.addCriteria(new Criteria().andOperator(
                Criteria.where("_id").is(a.get_id()),
                Criteria.where("userName").is(userName)
        ));
        mongoTemplate.updateFirst(queryA, Update.update("order", orderB), GoalStep.class);

        Query queryB = new Query();
        queryB.addCriteria(new Criteria().andOperator(
                Criteria.where("_id").is(b.get_id()),
                Criteria.where("userName").is(userName)
        ));
        mongoTemplate.updateFirst(queryB, Update.update("order", orderA), GoalStep.class);
    }
}
