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
                dotenv.get("AUCTION_DB_URL"),
                dotenv.get("DB_URL")
        );
        if (directUrl != null) {
            return directUrl;
        }

        String host = firstNonBlank(
                dotenv.get("AUCTION_DB_HOST"),
                dotenv.get("DB_HOST")
        );
        String port = firstNonBlank(
                dotenv.get("AUCTION_DB_PORT"),
                dotenv.get("DB_PORT")
        );
        String name = firstNonBlank(
                dotenv.get("AUCTION_DB_NAME"),
                dotenv.get("DB_NAME")
        );

        if (host == null || name == null) {
            return DEFAULT_JDBC_URL;
        }

        String resolvedPort = port == null ? "3306" : port;
        return "jdbc:mysql://" + host + ":" + resolvedPort + "/" + name;
    }

    private static String resolveDbUsername() {
        return firstNonBlank(
                dotenv.get("AUCTION_DB_USERNAME"),
                dotenv.get("DB_USER"),
                dotenv.get("DB_USERNAME"),
                DEFAULT_USERNAME
        );
    }

    private static String resolveDbPassword() {
        return firstNonBlank(
                dotenv.get("AUCTION_DB_PASSWORD"),
                dotenv.get("DB_PASSWORD"),
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
