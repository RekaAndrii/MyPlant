package com.my.plant.service;

import com.my.plant.model.Suggestion;

import java.util.List;

/**
 * Created by User on 27.07.2026.
 */
public interface SuggestionService {

    void save(Suggestion suggestion);

    List<Suggestion> getAll(String userName);
}
