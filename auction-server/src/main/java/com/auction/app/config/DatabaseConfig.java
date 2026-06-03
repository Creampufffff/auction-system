package com.auction.app.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final HikariDataSource dataSource;
    private static final String DEFAULT_JDBC_URL = "jdbc:mysql://localhost:3306/auction_system";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "123456";
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

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
                resolveConfig("AUCTION_DB_URL"),
                resolveConfig("DB_URL")
        );
        if (directUrl != null) {
            return directUrl;
        }

        String host = firstNonBlank(
                resolveConfig("AUCTION_DB_HOST"),
                resolveConfig("DB_HOST")
        );
        String port = firstNonBlank(
                resolveConfig("AUCTION_DB_PORT"),
                resolveConfig("DB_PORT")
        );
        String name = firstNonBlank(
                resolveConfig("AUCTION_DB_NAME"),
                resolveConfig("DB_NAME")
        );

        if (host == null || name == null) {
            return DEFAULT_JDBC_URL;
        }

        String resolvedPort = port == null ? "3306" : port;
        return "jdbc:mysql://" + host + ":" + resolvedPort + "/" + name;
    }

    private static String resolveDbUsername() {
        return firstNonBlank(
                resolveConfig("AUCTION_DB_USERNAME"),
                resolveConfig("DB_USER"),
                resolveConfig("DB_USERNAME"),
                DEFAULT_USERNAME
        );
    }

    private static String resolveDbPassword() {
        return firstNonBlank(
                resolveConfig("AUCTION_DB_PASSWORD"),
                resolveConfig("DB_PASSWORD"),
                DEFAULT_PASSWORD
        );
    }

    private static String resolveConfig(String key) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return dotenv.get(key);
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
