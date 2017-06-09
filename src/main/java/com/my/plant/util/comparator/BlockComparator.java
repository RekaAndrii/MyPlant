package com.my.plant.util.comparator;

import com.my.plant.model.Block;

import java.util.Comparator;

public class BlockComparator implements Comparator<Block>{
    
    @Override
    public int compare(Block o1, Block o2) {
        // Older first
        return o1.getLastExecution().compareTo(o2.getLastExecution());
    }
}
