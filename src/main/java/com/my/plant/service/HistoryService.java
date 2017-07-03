package com.my.plant.service;

import com.my.plant.model.HistoryItem;

import java.util.List;
public interface HistoryService {

    List<HistoryItem> getUserHistory(String username);

    void save(HistoryItem item);
}
