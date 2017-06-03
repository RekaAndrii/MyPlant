package com.my.plant.service.impl;

import com.my.plant.model.Block;
import com.my.plant.service.BlockService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 03.06.2017.
 */
@Service
public class BlockServiceImpl implements BlockService{
    private List<Block> blocks = new ArrayList<>();

    {
        blocks.add(new Block("Sport", "andrii", LocalDate.now(), LocalDate.now().minusDays(2)));
        blocks.add(new Block("Reading", "andrii", LocalDate.now(), LocalDate.now().minusDays(1)));
        blocks.add(new Block("Cool thing", "andrii", LocalDate.now(), LocalDate.now().minusDays(5)));
        blocks.add(new Block("Development", "andrii", LocalDate.now(), LocalDate.now()));
    }
    @Override
    public List<Block> getAllBlocks() {
        return blocks;
    }
}
