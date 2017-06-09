package com.my.plant.util.comparator;

import java.util.Comparator;

import com.my.plant.model.Block;

public class BlockComparator implements Comparator<Block>{
    
    @Override
    public int compare(Block o1, Block o2) {
        // Older first
        return o2.getLastExecution().compareTo(o1.getLastExecution());
    }
}
