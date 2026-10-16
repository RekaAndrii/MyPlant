package com.my.plant.controller;

import com.my.plant.model.Block;
import com.my.plant.service.BlockService;
import com.my.plant.service.HistoryService;
import com.my.plant.util.ColorUtil;
import com.my.plant.util.UserUtil;
import com.my.plant.util.comparator.BlockComparator;
import com.my.plant.util.constant.BlockColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by User on 21.04.2017.
 */
@Controller
public class HomeController {

    private final BlockComparator blockComparator = new BlockComparator(); 
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private BlockService blockService;

    @Autowired
    private HistoryService historyService;

    @RequestMapping(path = "home", method = RequestMethod.GET)
    public ModelAndView getHome(ModelAndView model){
        List<Block> blocks = Collections.emptyList();
        try {
            blocks = blockService.getAllBlocks(UserUtil.getLogginedUserName());
            ColorUtil.setColorToList(blocks);
            Collections.sort(blocks, blockComparator);
            long todayScore = blocks.stream()
                    .filter(b -> BlockColor.GREEN.equals(b.getColor()))
                    .count();
            model.addObject("todayScore", todayScore);
            LocalDate today = LocalDateTime.now().minusHours(3).toLocalDate();
            LocalDate weekAgo = today.minusDays(7);
            String userName = UserUtil.getLogginedUserName();
            model.addObject("yesterdayScore", historyService.countDistinctBlocksOnDate(userName, today.minusDays(1)));
            model.addObject("weekAgoScore", historyService.countDistinctBlocksOnDate(userName, weekAgo));
            model.addObject("weekAgoLabel", "Last " + weekAgo.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
        } catch (Exception ex) {
            LOGGER.error("Failed to load blocks for home page", ex);
            model.addObject("errorMessage", "Unable to load blocks right now. Please verify MongoDB connectivity and try again.");
            model.addObject("todayScore", 0L);
            model.addObject("yesterdayScore", 0);
            model.addObject("weekAgoScore", 0);
            model.addObject("weekAgoLabel", "Last week");
        }

        model.addObject("blocks", blocks);
        model.setViewName("index");
        return model;
    }
    @RequestMapping(path = "time", method = RequestMethod.GET)
    public @ResponseBody String getTime(ModelAndView model){

        return LocalDateTime.now().toString();
    }
}
