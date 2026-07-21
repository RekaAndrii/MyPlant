package com.my.plant.configs;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by User on 04.06.2017.
 */
@Configuration
public class MongoConfiguration extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.database:}")
    private String mongoDB;

    @Value("${spring.data.mongodb.uri:}")
    private String mongoUri;

    @Override
    protected String getDatabaseName() {
        return this.mongoDB;
    }

    @Override
    public MongoClient mongoClient() {
        if (StringUtils.hasText(mongoUri)) {
            return MongoClients.create(mongoUri);
        }

        return MongoClients.create();
    }

    @Override
    protected Collection<String> getMappingBasePackages() {
        return Collections.singleton("com.my.plant");
    }
}
