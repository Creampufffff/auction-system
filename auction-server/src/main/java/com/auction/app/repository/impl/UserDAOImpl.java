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

    private static final String USER_SELECT_SQL = """
            SELECT u.id,
                   u.username,
                   u.password,
                   u.email,
                   u.role,
                   CASE
                       WHEN u.role = 'SELLER' THEN COALESCE(s.balance, 0)
                       WHEN u.role = 'BIDDER' THEN COALESCE(b.balance, 0)
                       ELSE 0
                   END AS balance
            FROM users u
            LEFT JOIN seller s ON s.id = u.id
            LEFT JOIN bidder b ON b.id = u.id
            """;


    @Override
    public User findById(String id) {
        String sql = USER_SELECT_SQL + " WHERE u.id = ?";

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
        String sql = USER_SELECT_SQL + " ORDER BY u.username";
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
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);

            try {
                boolean saved = save(entity, connection);
                connection.commit();
                return saved;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save user: " + entity.getId(), e);
        }
    }

    boolean save(User entity, Connection connection) throws SQLException {
        String upsertUserSql = """
                INSERT INTO users (id, username, password, email, role)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    username = VALUES(username),
                    password = VALUES(password),
                    email = VALUES(email),
                    role = VALUES(role)
                """;

        try (PreparedStatement statement = connection.prepareStatement(upsertUserSql)) {
            statement.setString(1, entity.getId());
            statement.setString(2, entity.getUsername());
            statement.setString(3, entity.getPassword());
            statement.setString(4, entity.getEmail());
            statement.setString(5, resolveRole(entity));
            statement.executeUpdate();
        }

        syncSubtypeTables(entity, connection);
        return true;
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
        String sql = USER_SELECT_SQL + " WHERE u.username = ?";

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

    private void syncSubtypeTables(User entity, Connection connection) throws SQLException {
        try (PreparedStatement deleteAdmin = connection.prepareStatement("DELETE FROM admin WHERE id = ?");
             PreparedStatement deleteSeller = connection.prepareStatement("DELETE FROM seller WHERE id = ?");
             PreparedStatement deleteBidder = connection.prepareStatement("DELETE FROM bidder WHERE id = ?")) {
            deleteAdmin.setString(1, entity.getId());
            deleteSeller.setString(1, entity.getId());
            deleteBidder.setString(1, entity.getId());
            deleteAdmin.executeUpdate();
            deleteSeller.executeUpdate();
            deleteBidder.executeUpdate();
        }

        if (entity instanceof Admin) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO admin (id) VALUES (?) ON DUPLICATE KEY UPDATE id = VALUES(id)")) {
                statement.setString(1, entity.getId());
                statement.executeUpdate();
            }
            return;
        }

        String subtypeSql;
        if (entity instanceof Seller) {
            subtypeSql = "INSERT INTO seller (id, balance) VALUES (?, ?) ON DUPLICATE KEY UPDATE balance = VALUES(balance)";
        } else {
            subtypeSql = "INSERT INTO bidder (id, balance) VALUES (?, ?) ON DUPLICATE KEY UPDATE balance = VALUES(balance)";
        }

        try (PreparedStatement statement = connection.prepareStatement(subtypeSql)) {
            statement.setString(1, entity.getId());
            statement.setDouble(2, entity.getBalance());
            statement.executeUpdate();
        }
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
