package com.my.plant.repository;

import com.my.plant.model.Block;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by User on 04.06.2017.
 */

@Repository
public interface BlockRepository extends MongoRepository<Block, String> {
}
