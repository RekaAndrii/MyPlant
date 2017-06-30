package com.my.plant.service.impl;

import com.my.plant.model.Block;
import com.my.plant.service.BlockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 03.06.2017.
 */
@Service
public class BlockServiceImpl implements BlockService{
    private List<Block> blocks = new ArrayList<>();

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<Block> getAllBlocks() {
        return mongoTemplate.findAll(Block.class);
    }

    @Override
    public Block findByName(String name) {
        Query query = new Query();
        query.addCriteria(Criteria.where("name").is(name));
        return mongoTemplate.findOne(query, Block.class);
    }

    @Override
    public void save(Block block) {
        mongoTemplate.save(block);
    }

    @Override
    public void remove(String name) {
        Query query = new Query();
        query.addCriteria(Criteria.where("name").is(name));
        mongoTemplate.remove(query, Block.class);
    }
}
