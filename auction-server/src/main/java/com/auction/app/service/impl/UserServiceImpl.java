package com.auction.app.service.impl;

import com.app.common.entity.User;
import com.app.common.exception.UserAuthException;
import com.auction.app.repository.UserDAO;
import com.auction.app.service.UserService;

import java.util.List;

public class UserServiceImpl implements UserService {
    private final UserDAO userDAO;

    public UserServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public void register(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Người dùng không được null");
        }

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống");
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email không được để trống");
        }

        User existingUser = userDAO.findByUsername(user.getUsername());

        if (existingUser != null) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }

        if (!userDAO.save(user)) {
            throw new IllegalStateException("Không thể lưu người dùng");
        }
    }

    @Override
    public User login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }

        User user = userDAO.findByUsername(username);

        if (user == null) {
            throw new UserAuthException("Không tìm thấy người dùng");
        }

        if (!user.getPassword().equals(password)) {
            throw new UserAuthException("Mật khẩu không đúng");
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
            throw new IllegalArgumentException("Không tìm thấy người dùng");
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
        if (!userDAO.save(user)) {
            throw new IllegalStateException("Không thể cập nhật số dư");
        }
    }

    @Override
    public void withdraw(String userId, double amount) {
        User user = getRequiredUser(userId);
        user.withdraw(amount);
        if (!userDAO.save(user)) {
            throw new IllegalStateException("Không thể cập nhật số dư");
        }
    }

    private void validateId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("UserId không được để trống");
        }
    }

    private User getRequiredUser(String userId) {
        validateId(userId);
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Không tìm thấy người dùng");
        }
        return user;
    }
}
