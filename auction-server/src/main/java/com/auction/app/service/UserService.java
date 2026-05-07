package com.auction.app.service;

import com.app.common.entity.User;

import java.util.List;

public interface UserService {
    void register(User user);

    User login(String username, String password);

    User getById(String UserId);

    List<User> getAllUser();

    void deleteUser(String userId);

    double getBalance(String userId);

    void deposit(String userId, double amount);

    void withdraw(String userId, double amount);
}
