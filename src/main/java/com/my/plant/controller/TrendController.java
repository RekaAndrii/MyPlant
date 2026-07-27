package com.my.plant.controller;

import com.my.plant.service.TrendService;
import com.my.plant.util.dto.TrendDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Created by User on 30.06.2017.
 */
@Controller
public class TrendController {

    @Autowired
    private TrendService trendService;

    @RequestMapping(value = "/trend",  method = RequestMethod.GET)
    public ModelAndView trend(ModelAndView model){
        model.setViewName("trend");
        return model;
    }

    @GetMapping(path = "/trend/countPerDay")
    public @ResponseBody TrendDto<Map<DayOfWeek, Map<String, Integer>>> getCountPerDay(@RequestParam(required = false) String time) {
        TrendDto<Map<DayOfWeek, Map<String, Integer>>> trend;
        if(time != null && time.toUpperCase().equals("MONTH")){
            trend = trendService.getBlockTrendPerDay(LocalDate.now().minusMonths(1));
        }else{
             trend = trendService.getBlockTrendPerDay(null);
        }
        return trend;
    }

    @GetMapping(path = "/trend/countPerDate")
    public @ResponseBody List<Map<String, Object>> getCountPerDate() {
        return trendService.getCountPerDate(LocalDate.now().minusDays(29));
    }
}
