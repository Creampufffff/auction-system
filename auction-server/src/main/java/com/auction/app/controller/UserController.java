package com.auction.app.controller;

import com.app.common.entity.User;
import com.auction.app.service.UserService;

public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public void register(User user) {
        userService.register(user);
    }

    public User login(String username, String password) {
        return userService.login(username, password);
    }

    public User getUserProfile(String userId) {
        return userService.getById(userId);
    }

    public double getBalance(String userId) {
        return userService.getBalance(userId);
    }

    public void deposit(String userId, double amount) {
        userService.deposit(userId, amount);
    }

    public void withdraw(String userId, double amount) {
        userService.withdraw(userId, amount);
    }

    public void deleteUser(String userId) {
        userService.deleteUser(userId);
    }
}


