package com.my.plant.util.comparator;

import com.my.plant.model.Block;

import java.time.LocalDate;
import java.util.Comparator;

public class BlockComparator implements Comparator<Block>{
    private final int PLUS = 1;
    private final int MINUS = -1;
    private final int EQUAL = 0;


    @Override
    public int compare(Block block1, Block block2) {
        LocalDate date1 = block1.getLastExecution();
        LocalDate date2 = block2.getLastExecution();
        // Older first
        if(date1 == null && date2 == null){
            return EQUAL;
        }else{
            if(date1 == null){
                return MINUS;
            }
            if(date2 == null){
                return PLUS;
            }
        }
        return block1.getLastExecution().compareTo(block2.getLastExecution());
    }
}
