package com.auction.app.service.impl;

import com.app.common.entity.User;
import com.app.common.exception.UserAuthException;
import com.auction.app.repository.UserDAO;
import com.auction.app.service.UserService;
import com.auction.app.service.security.PasswordHasher;

import java.util.List;

public class UserServiceImpl implements UserService {
    private final UserDAO userDAO;

    public UserServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public void register(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        User existingUser = userDAO.findByUsername(user.getUsername());

        if (existingUser != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        user.setPasswordHash(PasswordHasher.hash(user.getPasswordHash()));

        if (!userDAO.save(user)) {
            throw new IllegalStateException("Failed to save user");
        }
    }

    @Override
    public User login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        User user = userDAO.findByUsername(username);

        if (user == null) {
            throw new UserAuthException("User not found");
        }

        if (!PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new UserAuthException("Incorrect password");
        }

        return user;
    }

    @Override
    public User getById(String userId) {
        validateId(userId);
        return userDAO.findById(userId);
    }

    @Override
    public List<User> getAllUser() {
        return userDAO.findAll();
    }

    @Override
    public void deleteUser(String userId) {
        validateId(userId);
        if (!userDAO.delete(userId)) {
            throw new IllegalArgumentException("User not found");
        }
    }

    @Override
    public double getBalance(String userId) {
        User user = getRequiredUser(userId);
        return user.getBalance();
    }

    @Override
    public void deposit(String userId, double amount) {
        User user = getRequiredUser(userId);
        user.deposit(amount);
        if (!userDAO.updateBalance(user)) {
            throw new IllegalStateException("Failed to update balance");
        }
    }

    @Override
    public void withdraw(String userId, double amount) {
        User user = getRequiredUser(userId);
        user.withdraw(amount);
        if (!userDAO.updateBalance(user)) {
            throw new IllegalStateException("Failed to update balance");
        }
    }

    private void validateId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
    }

    private User getRequiredUser(String userId) {
        validateId(userId);
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return user;
    }
}
