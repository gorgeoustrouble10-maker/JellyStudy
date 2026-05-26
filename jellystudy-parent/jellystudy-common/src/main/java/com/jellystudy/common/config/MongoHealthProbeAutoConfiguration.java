package com.jellystudy.common.config;

import com.jellystudy.common.health.HealthProbe;
import com.jellystudy.common.health.MongoHealthProbe;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

@AutoConfiguration(after = MongoAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.data.mongodb.core.MongoTemplate")
public class MongoHealthProbeAutoConfiguration {

    @Bean
    @ConditionalOnBean(MongoTemplate.class)
    HealthProbe mongoHealthProbe(MongoTemplate mongoTemplate) {
        return new MongoHealthProbe(mongoTemplate);
    }
}
