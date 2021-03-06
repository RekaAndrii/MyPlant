package com.my.plant.service;

import com.my.plant.model.Block;

import java.util.List;

/**
 * Created by User on 03.06.2017.
 */
public interface BlockService {

    List<Block> getAllBlocks(String username);

    Block findByName(String name, String username);

    void save(Block block);

    void remove(String name, String username);

}
