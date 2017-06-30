package com.my.plant.util.comparator;

import com.my.plant.model.Block;

import java.time.LocalDate;
import java.util.Comparator;

public class BlockComparator implements Comparator<Block>{
    private final int PLUS = 1;
    private final int MINUS = -1;
    private final int EQUAL = 0;


    @Override
    public int compare(Block o1, Block o2) {
        LocalDate date1 = o1.getLastExecution();
        LocalDate date2 = o2.getLastExecution();
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
        return o1.getLastExecution().compareTo(o2.getLastExecution());
    }
}
