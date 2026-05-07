package com.auction.app.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final HikariDataSource dataSource;
    private static final String DEFAULT_JDBC_URL = "jdbc:mysql://localhost:3306/auction_system";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "123456";

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(resolveJdbcUrl());
        config.setUsername(resolveDbUsername());
        config.setPassword(resolveDbPassword());
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private static String resolveJdbcUrl() {
        String directUrl = firstNonBlank(
                System.getenv("AUCTION_DB_URL"),
                System.getenv("DB_URL")
        );
        if (directUrl != null) {
            return directUrl;
        }

        String host = firstNonBlank(
                System.getenv("AUCTION_DB_HOST"),
                System.getenv("DB_HOST")
        );
        String port = firstNonBlank(
                System.getenv("AUCTION_DB_PORT"),
                System.getenv("DB_PORT")
        );
        String name = firstNonBlank(
                System.getenv("AUCTION_DB_NAME"),
                System.getenv("DB_NAME")
        );

        if (host == null || name == null) {
            return DEFAULT_JDBC_URL;
        }

        String resolvedPort = port == null ? "3306" : port;
        return "jdbc:mysql://" + host + ":" + resolvedPort + "/" + name;
    }

    private static String resolveDbUsername() {
        return firstNonBlank(
                System.getenv("AUCTION_DB_USERNAME"),
                System.getenv("DB_USER"),
                System.getenv("DB_USERNAME"),
                DEFAULT_USERNAME
        );
    }

    private static String resolveDbPassword() {
        return firstNonBlank(
                System.getenv("AUCTION_DB_PASSWORD"),
                System.getenv("DB_PASSWORD"),
                DEFAULT_PASSWORD
        );
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
