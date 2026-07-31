package com.my.plant.service.impl;

import com.my.plant.model.Achievement;
import com.my.plant.model.Block;
import com.my.plant.model.Goal;
import com.my.plant.model.GoalStep;
import com.my.plant.model.HistoryItem;
import com.my.plant.model.User;
import com.my.plant.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Created by User on 23.07.2026.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final int MIN_PASSWORD_LENGTH = 4;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public boolean emailExists(String email) {
        Query query = new Query();
        query.addCriteria(Criteria.where("email").is(email));
        return mongoTemplate.findOne(query, User.class) != null;
    }

    @Override
    public void register(String userName, String email, String rawPassword) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least 4 characters long.");
        }
        if (emailExists(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        String encodedPassword = passwordEncoder.encode(rawPassword);
        mongoTemplate.save(new User(userName, email, encodedPassword));
    }

    @Override
    public void deleteCurrentUser(String userName) {
        // Delete all blocks belonging to this user
        mongoTemplate.remove(new Query(Criteria.where("userName").is(userName)), Block.class);
        // Delete all history items belonging to this user
        mongoTemplate.remove(new Query(Criteria.where("userName").is(userName)), HistoryItem.class);
        // Delete all achievements belonging to this user
        mongoTemplate.remove(new Query(Criteria.where("userName").is(userName)), Achievement.class);
        // Delete all goal steps belonging to this user
        mongoTemplate.remove(new Query(Criteria.where("userName").is(userName)), GoalStep.class);
        // Delete all goals belonging to this user
        mongoTemplate.remove(new Query(Criteria.where("userName").is(userName)), Goal.class);
        // Delete the user record
        mongoTemplate.remove(new Query(Criteria.where("userName").is(userName)), User.class);
    }
}
