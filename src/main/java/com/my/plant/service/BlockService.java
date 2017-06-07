package com.my.plant.service;

import com.my.plant.model.Block;

import java.util.List;

/**
 * Created by User on 03.06.2017.
 */
public interface BlockService {

    List<Block> getAllBlocks();

    Block findByName(String name);

    void save(Block block);

}
