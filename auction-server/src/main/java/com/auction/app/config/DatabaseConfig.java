package com.auction.app.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final HikariDataSource dataSource;
    private static final String DEFAULT_JDBC_URL = "jdbc:mysql://localhost:3306/auction_system";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "";

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getEnvOrDefault("AUCTION_DB_URL", DEFAULT_JDBC_URL));
        config.setUsername(getEnvOrDefault("AUCTION_DB_USERNAME", DEFAULT_USERNAME));
        config.setPassword(getEnvOrDefault("AUCTION_DB_PASSWORD", DEFAULT_PASSWORD));
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
