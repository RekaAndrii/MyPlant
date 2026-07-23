package com.my.plant.service;

/**
 * Created by User on 23.07.2026.
 */
public interface UserService {

    boolean emailExists(String email);

    void register(String userName, String email, String rawPassword);

    void deleteCurrentUser(String userName);
}
