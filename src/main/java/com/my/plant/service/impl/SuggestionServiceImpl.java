package com.my.plant.service.impl;

import com.my.plant.model.Suggestion;
import com.my.plant.service.SuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by User on 27.07.2026.
 */
@Service
public class SuggestionServiceImpl implements SuggestionService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void save(Suggestion suggestion) {
        mongoTemplate.save(suggestion);
    }

    @Override
    public List<Suggestion> getAll(String userName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userName").is(userName));
        query.with(Sort.by(Sort.Direction.DESC, "createdDate"));
        return mongoTemplate.find(query, Suggestion.class);
    }
}
