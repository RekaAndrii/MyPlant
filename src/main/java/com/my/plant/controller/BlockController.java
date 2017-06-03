package com.my.plant.controller;

import com.my.plant.util.dto.AjaxResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by User on 04.06.2017.
 */
@Controller
@ResponseBody
@RequestMapping("/block")
public class BlockController {

    @RequestMapping(value = "/execute", method = RequestMethod.GET)
    public @ResponseBody AjaxResponse executeBlock(@RequestParam(value = "name", required = false) String name){

        System.out.println(name);
        return new AjaxResponse(false, "Successfully executed");
    }

}
