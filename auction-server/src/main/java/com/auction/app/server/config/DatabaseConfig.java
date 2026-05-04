package com.auction.app.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        // Sửa lại URL, user, pass cho đúng với máy của ông
        config.setJdbcUrl("jdbc:mysql://localhost:3306/auction_system");
        config.setUsername("root");
        config.setPassword("123456");
        config.setMaximumPoolSize(10); // Cho phép 10 luồng kết nối cùng lúc
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}