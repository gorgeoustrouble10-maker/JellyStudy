package com.jellystudy.common.health;

import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.sql.Connection;

@RequiredArgsConstructor
public class DataSourceHealthProbe implements HealthProbe {

    private final DataSource dataSource;

    @Override
    public String component() {
        return "mysql";
    }

    @Override
    public boolean isUp() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
