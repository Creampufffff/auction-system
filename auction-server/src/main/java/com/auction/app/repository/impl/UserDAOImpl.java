package com.auction.app.repository.impl;

import com.app.common.entity.Admin;
import com.app.common.entity.Bidder;
import com.app.common.entity.Seller;
import com.app.common.entity.User;
import com.auction.app.config.DatabaseConfig;
import com.auction.app.repository.UserDAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements UserDAO {
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SELLER = "SELLER";
    private static final String ROLE_BIDDER = "BIDDER";


    @Override
    public User findById(String id) {
        String sql = "SELECT id, username, password, email, role, balance FROM users WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot find user by id: " + id, e);
        }

        return null;
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT id, username, password, email, role, balance FROM users ORDER BY username";
        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load users", e);
        }

        return users;
    }

    @Override
    public boolean save(User entity) {
        String sql = """
                INSERT INTO users (id, username, password, email, role, balance)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    username = VALUES(username),
                    password = VALUES(password),
                    email = VALUES(email),
                    role = VALUES(role),
                    balance = VALUES(balance)
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entity.getId());
            statement.setString(2, entity.getUsername());
            statement.setString(3, entity.getPassword());
            statement.setString(4, entity.getEmail());
            statement.setString(5, resolveRole(entity));
            statement.setDouble(6, entity.getBalance());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save user: " + entity.getId(), e);
        }
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot delete user: " + id, e);
        }
    }

    @Override
    public User findByUsername(String username) {
        String sql = "SELECT id, username, password, email, role, balance FROM users WHERE username = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot find user by username: " + username, e);
        }

        return null;
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        String role = resultSet.getString("role");
        User user;

        if (ROLE_ADMIN.equals(role)) {
            user = new Admin(
                    resultSet.getString("username"),
                    resultSet.getString("password"),
                    resultSet.getString("email")
            );
        } else if (ROLE_SELLER.equals(role)) {
            user = new Seller(
                    resultSet.getString("username"),
                    resultSet.getString("password"),
                    resultSet.getString("email")
            );
        } else {
            user = new Bidder(
                    resultSet.getString("username"),
                    resultSet.getString("password"),
                    resultSet.getString("email")
            );
        }

        user.setId(resultSet.getString("id"));
        user.setBalance(resultSet.getDouble("balance"));
        return user;
    }

    private String resolveRole(User user) {
        if (user instanceof Admin) {
            return ROLE_ADMIN;
        }
        if (user instanceof Seller) {
            return ROLE_SELLER;
        }
        return ROLE_BIDDER;
    }
}
