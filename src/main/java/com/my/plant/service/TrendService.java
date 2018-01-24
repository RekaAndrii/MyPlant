package com.my.plant.service;

import com.my.plant.util.dto.TrendDto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

/**
 * Created by User on 01.07.2017.
 */
public interface TrendService {

    TrendDto<Map<DayOfWeek, Map<String, Integer>>> getBlockTrendPerDay(LocalDate since);
}
