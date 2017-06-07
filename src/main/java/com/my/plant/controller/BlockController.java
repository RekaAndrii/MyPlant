package com.my.plant.controller;

import com.my.plant.model.Block;
import com.my.plant.service.BlockService;
import com.my.plant.util.dto.AjaxResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;

/**
 * Created by User on 04.06.2017.
 */
@Controller
@ResponseBody
@RequestMapping("/block")
public class BlockController {

    @Autowired
    private BlockService blockService;

    @RequestMapping(value = "/execute", method = RequestMethod.GET)
    public @ResponseBody AjaxResponse executeBlock(@RequestParam(value = "name", required = false) String name){
        Block block = blockService.findByName(name);
        if(block != null){
            block.setLastExecution(LocalDate.now());
            blockService.save(block);
        }
        return new AjaxResponse(false, "Successfully executed");
    }

}