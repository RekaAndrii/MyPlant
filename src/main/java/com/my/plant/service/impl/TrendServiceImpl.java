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
import java.time.format.DateTimeFormatter;
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

    @Override
    public List<Map<String, Object>> getCountPerDate(LocalDate since) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate from = since != null ? since : LocalDate.now().minusDays(29);

        List<HistoryItem> historyItems = historyService.getUserHistory(UserUtil.getLogginedUserName());
        historyItems = historyItems.stream()
                .filter(item -> !item.getTime().toLocalDate().isBefore(from))
                .collect(Collectors.toList());

        // Count executions per calendar date
        Map<LocalDate, Integer> countByDate = new TreeMap<>();
        historyItems.forEach(item -> {
            LocalDate date = item.getTime().toLocalDate();
            countByDate.put(date, countByDate.getOrDefault(date, 0) + 1);
        });

        // Fill in every date in the range with 0 if missing, so the line chart has no gaps
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(LocalDate.now())) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", cursor.format(formatter));
            point.put("count", countByDate.getOrDefault(cursor, 0));
            result.add(point);
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private void initWeekDays(Map<DayOfWeek, Map<String, Integer>> map){
        List<DayOfWeek> dayOfWeeks = Arrays.asList(DayOfWeek.values());
        dayOfWeeks.forEach(dayOfWeek ->
            map.put(dayOfWeek, new TreeMap<String, Integer>())
        );

    }
}
