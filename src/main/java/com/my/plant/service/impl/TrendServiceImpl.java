package com.my.plant.service.impl;

import com.my.plant.model.HistoryItem;
import com.my.plant.service.HistoryService;
import com.my.plant.service.TrendService;
import com.my.plant.util.UserUtil;
import com.my.plant.util.dto.TrendDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by User on 01.07.2017.
 */
@Service
public class TrendServiceImpl implements TrendService {

    @Autowired
    private HistoryService historyService;


    public TrendDto<Map<DayOfWeek, Map<String, Integer>>> getBlockTrendPerDay(LocalDate since){
        Map<DayOfWeek, Map<String, Integer>> activitiesPerDay = new TreeMap<>();
        initWeekDays(activitiesPerDay);
        Set<String> yValues = new HashSet<>();
        List<HistoryItem> historyItems = historyService.getUserHistory(UserUtil.getLogginedUserName());
        if(since != null){
            historyItems = historyItems.stream().filter(item -> item.getTime().toLocalDate().isAfter(since)).collect(Collectors.toList());
        }
        historyItems.forEach(historyItem -> {
           Map<String, Integer> activitiesCountMap = activitiesPerDay.get(historyItem.getTime().getDayOfWeek());
            yValues.add(historyItem.getBlockName());
            Integer count = activitiesCountMap.get(historyItem.getBlockName());
            if (count == null){
                count = 1;
                activitiesCountMap.put(historyItem.getBlockName(), count);
            }else {
                activitiesCountMap.put(historyItem.getBlockName(), ++count);
            }
        });

        return new TrendDto<>(activitiesPerDay, yValues.stream().collect(Collectors.toList()));
    }

    private void initWeekDays(Map<DayOfWeek, Map<String, Integer>> map){
        List<DayOfWeek> dayOfWeeks = Arrays.asList(DayOfWeek.values());
        dayOfWeeks.forEach(dayOfWeek ->
            map.put(dayOfWeek, new TreeMap<String, Integer>())
        );

    }
}
