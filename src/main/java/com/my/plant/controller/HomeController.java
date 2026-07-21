package com.my.plant.controller;

import com.my.plant.model.Block;
import com.my.plant.service.BlockService;
import com.my.plant.util.ColorUtil;
import com.my.plant.util.UserUtil;
import com.my.plant.util.comparator.BlockComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Created by User on 21.04.2017.
 */
@Controller
public class HomeController {

    private final BlockComparator blockComparator = new BlockComparator(); 
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private BlockService blockService;

    @RequestMapping(path = "home", method = RequestMethod.GET)
    public ModelAndView getHome(ModelAndView model){
        List<Block> blocks = Collections.emptyList();
        try {
            blocks = blockService.getAllBlocks(UserUtil.getLogginedUserName());
            ColorUtil.setColorToList(blocks);
            Collections.sort(blocks, blockComparator);
        } catch (Exception ex) {
            LOGGER.error("Failed to load blocks for home page", ex);
            model.addObject("errorMessage", "Unable to load blocks right now. Please verify MongoDB connectivity and try again.");
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
