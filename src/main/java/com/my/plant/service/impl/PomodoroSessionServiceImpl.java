package com.my.plant.service.impl;

import com.my.plant.model.PomodoroSession;
import com.my.plant.service.PomodoroSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PomodoroSessionServiceImpl implements PomodoroSessionService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void save(PomodoroSession session) {
        mongoTemplate.save(session);
    }

    @Override
    public List<PomodoroSession> getAll(String userName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userName").is(userName));
        query.with(Sort.by(Sort.Direction.DESC, "endedAt"));
        return mongoTemplate.find(query, PomodoroSession.class);
    }

    @Override
    public void delete(String id, String userName) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("_id").is(id),
                Criteria.where("userName").is(userName)
        ));
        mongoTemplate.remove(query, PomodoroSession.class);
    }

    @Override
    public void updateTags(String id, String userName, List<String> goalStepIds, List<String> blockNames) {
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("_id").is(id),
                Criteria.where("userName").is(userName)
        ));

        Update update = new Update();
        update.set("goalStepIds", goalStepIds);
        update.set("blockNames", blockNames);
        mongoTemplate.updateFirst(query, update, PomodoroSession.class);
    }
}
