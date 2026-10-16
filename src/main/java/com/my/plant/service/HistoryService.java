package com.my.plant.service;

import com.my.plant.model.HistoryItem;

import java.time.LocalDate;
import java.util.List;
public interface HistoryService {

    List<HistoryItem> getUserHistory(String username);

    void save(HistoryItem item);

    /**
     * Number of distinct blocks executed on the given date, using the same
     * 3-hour shift that BlockController applies to lastExecution.
     */
    int countDistinctBlocksOnDate(String username, LocalDate date);

    void renameBlock(String oldName, String newName, String userName);
}
