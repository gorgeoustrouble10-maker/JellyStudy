package com.jellystudy.common.health;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

@RequiredArgsConstructor
public class MongoHealthProbe implements HealthProbe {

    private final MongoTemplate mongoTemplate;

    @Override
    public String component() {
        return "mongodb";
    }

    @Override
    public boolean isUp() {
        try {
            mongoTemplate.executeCommand("{ ping: 1 }");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
