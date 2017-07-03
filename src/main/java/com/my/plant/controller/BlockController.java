package com.my.plant.controller;

import com.my.plant.model.Block;
import com.my.plant.model.HistoryAction;
import com.my.plant.model.HistoryItem;
import com.my.plant.service.BlockService;
import com.my.plant.service.HistoryService;
import com.my.plant.util.UserUtil;
import com.my.plant.util.dto.AjaxResponse;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by User on 04.06.2017.
 */
@Controller
@ResponseBody
@RequestMapping("/block")
public class BlockController {

    @Autowired
    private HistoryService historyService;

    @Autowired
    private BlockService blockService;

    @RequestMapping(value = "/execute", method = RequestMethod.GET)
    @ApiOperation(value = "executeBlock", nickname = "executeBlock")
    public @ResponseBody AjaxResponse executeBlock(@RequestParam(value = "name", required = false) String name){
        String userName = UserUtil.getLogginedUserName();
        Block block = blockService.findByName(name, userName);
        if(block != null){

            block.setLastExecution(LocalDate.now());
            blockService.save(block);
            historyService.save(new HistoryItem(block.getUserName(), block.getName(), HistoryAction.EXECUTED,
                    LocalDateTime.now()));
        }
        return new AjaxResponse(false, "Successfully executed");
    }

    @GetMapping("/all")
    public @ResponseBody List<Block> getAll(){
       return blockService.getAllBlocks(UserUtil.getLogginedUserName());
    }

    @PostMapping(value = "/")
    public @ResponseBody AjaxResponse create(@RequestBody  Block block){
        block.setCreationDate(LocalDate.now());
        blockService.save(block);
        return new AjaxResponse(false, "Successfully executed");
    }

    @DeleteMapping(value = "/{name}")
    public @ResponseBody AjaxResponse delete(@PathVariable(name = "name") String blockName){
        blockService.remove(blockName, UserUtil.getLogginedUserName());
        return new AjaxResponse(false, "Successfully executed");
    }


}
