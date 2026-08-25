package com.my.plant.controller;

import com.my.plant.model.Achievement;
import com.my.plant.model.Block;
import com.my.plant.model.HistoryAction;
import com.my.plant.model.HistoryItem;
import com.my.plant.service.AchievementService;
import com.my.plant.service.BlockService;
import com.my.plant.service.GoalStepService;
import com.my.plant.service.HistoryService;
import com.my.plant.util.UserUtil;
import com.my.plant.util.dto.AjaxResponse;
import com.my.plant.util.dto.ExecuteBlockResponse;
import io.swagger.v3.oas.annotations.Operation;
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

    private static final String AJAX_OK_MESSAGE = "ok";

    @Autowired
    private HistoryService historyService;

    @Autowired
    private BlockService blockService;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private GoalStepService goalStepService;

    @RequestMapping(value = "/execute", method = RequestMethod.GET)
    @Operation(summary = "executeBlock", operationId = "executeBlock")
    public @ResponseBody ExecuteBlockResponse executeBlock(@RequestParam(value = "name", required = false) String name) {
        String userName = UserUtil.getLogginedUserName();
        Block block = blockService.findByName(name, userName);
        if (block != null && !block.isDisabled()) {
            block.setLastExecution(LocalDateTime.now().minusHours(3).toLocalDate());

            boolean completed = false;
            if (block.isChallenge() && block.getRemainingExecutions() != null) {
                int remaining = block.getRemainingExecutions() - 1;
                if (remaining <= 0) {
                    remaining = 0;
                    completed = true;
                    block.setCompleted(true);
                    achievementService.save(new Achievement(
                            userName,
                            block.getName(),
                            block.getTargetExecutions(),
                            LocalDateTime.now().minusHours(3).toLocalDate()
                    ));
                    goalStepService.markDoneByBlockName(block.getName(), userName);
                }
                block.setRemainingExecutions(remaining);
            }

            blockService.save(block);
            historyService.save(new HistoryItem(userName, block.getName(), HistoryAction.EXECUTED,
                    LocalDateTime.now()));

            return new ExecuteBlockResponse(false, AJAX_OK_MESSAGE, block.isChallenge(),
                    block.getRemainingExecutions(), completed);
        }
        return new ExecuteBlockResponse(false, AJAX_OK_MESSAGE, false, null, false);
    }

    @GetMapping("/all")
    public @ResponseBody List<Block> getAll() {
        return blockService.getAllBlocks(UserUtil.getLogginedUserName());
    }

    @PostMapping(value = "/")
    public @ResponseBody AjaxResponse create(@RequestBody Block block) {
        block.setCreationDate(LocalDate.now());
        block.setUserName(UserUtil.getLogginedUserName());
        if (block.isChallenge() && block.getTargetExecutions() != null) {
            block.setRemainingExecutions(block.getTargetExecutions());
        }
        blockService.save(block);
        return new AjaxResponse(false, AJAX_OK_MESSAGE);
    }

    @PutMapping(value = "/{name}")
    public @ResponseBody AjaxResponse update(@PathVariable(name = "name") String oldName,
                                             @RequestBody Block block) {
        String userName = UserUtil.getLogginedUserName();
        blockService.update(oldName, block, userName);
        if (!oldName.equals(block.getName())) {
            historyService.renameBlock(oldName, block.getName(), userName);
        }
        return new AjaxResponse(false, AJAX_OK_MESSAGE);
    }

    @DeleteMapping(value = "/{name}")
    public @ResponseBody AjaxResponse delete(@PathVariable(name = "name") String blockName) {
        blockService.remove(blockName, UserUtil.getLogginedUserName());
        return new AjaxResponse(false, AJAX_OK_MESSAGE);
    }

    @PutMapping(value = "/{name}/disabled")
    @Operation(summary = "setBlockDisabled", operationId = "setBlockDisabled")
    public @ResponseBody AjaxResponse setDisabled(@PathVariable(name = "name") String blockName,
                                                   @RequestParam(name = "disabled") boolean disabled) {
        blockService.updateDisabled(blockName, disabled, UserUtil.getLogginedUserName());
        return new AjaxResponse(false, AJAX_OK_MESSAGE);
    }
}

