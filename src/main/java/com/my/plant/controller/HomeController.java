package com.my.plant.controller;

import com.my.plant.model.Block;
import com.my.plant.service.BlockService;
import com.my.plant.util.ColorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * Created by User on 21.04.2017.
 */
@Controller
public class HomeController {

    @Autowired
    private BlockService blockService;

    @RequestMapping(path = "home", method = RequestMethod.GET)
    public ModelAndView getHome(ModelAndView model){
        List<Block> blocks =  blockService.getAllBlocks();
        ColorUtil.setColorToList(blocks);
        model.addObject("blocks", blocks);
        model.setViewName("index");
        return model;
    }
}
