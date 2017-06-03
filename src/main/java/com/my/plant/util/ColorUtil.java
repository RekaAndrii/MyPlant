package com.my.plant.util;

import com.my.plant.model.Block;
import com.my.plant.util.constant.BlockColor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Created by User on 03.06.2017.
 */
public class ColorUtil {
    private final static int green = 0;
    private final static int yellow = 1;
    private final static int red = 2;

    public static void setColor(Block block){
        LocalDate now = LocalDate.now();
        LocalDate blockDate = block.getLastExecution();
        long differnce = ChronoUnit.DAYS.between(blockDate, now);
        if(differnce == green){
            block.setColor(BlockColor.GREEN);
        }else if(differnce > green && differnce <= yellow){
            block.setColor(BlockColor.YELLOW);
        }else if(differnce >= red){
            block.setColor(BlockColor.RED);
        }
    }

    public static void setColorToList(List<Block> blocks){
        for (Block block : blocks) {
            setColor(block);
        }
    }
}
