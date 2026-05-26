package com.jellystudy.common.config;

import com.jellystudy.common.health.DataSourceHealthProbe;
import com.jellystudy.common.health.HealthProbe;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@AutoConfiguration(after = DataSourceAutoConfiguration.class)
public class DataSourceHealthProbeAutoConfiguration {

    @Bean
    @ConditionalOnBean(DataSource.class)
    HealthProbe dataSourceHealthProbe(DataSource dataSource) {
        return new DataSourceHealthProbe(dataSource);
    }
}
