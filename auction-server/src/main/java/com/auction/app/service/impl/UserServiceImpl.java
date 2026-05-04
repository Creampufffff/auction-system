package src.main.java.com.auction.app.service.impl;

import src.main.java.com.app.common.entity.User;
import src.main.java.com.auction.app.repository.UserDAO;
import src.main.java.com.auction.app.service.UserService;

import java.util.List;

public class UserServiceImpl implements UserService {
    private final UserDAO userDAO;

    public UserServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public void register(User user) {
        if (user == null){
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getUsername() == null || user.getUsername().isBlank()){
            throw new IllegalArgumentException("Username cannot be empty");
        };

        if (user.getPassword() == null || user.getPassword().isBlank()){
            throw new IllegalArgumentException("Password cannot be empty");
        }

        User existinguser = userDAO.findByUsername(user.getUsername());

        if (existinguser != null){
            throw new IllegalArgumentException("Username already exist");
        }

        userDAO.save(user);
    }

    @Override
    public User login(String username, String password) {
        if (username == null || username.isBlank()){
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (password == null || password.isBlank()){
            throw new IllegalArgumentException("Password cannot be empty");
        }

        User user = userDAO.findByUsername(username);

        if (user == null){
            throw new IllegalArgumentException("User not found");
        }

        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Wrong password");
        }

        return user;
    }

    @Override
    public User getById(String userId) {
        if (userId == null || userId.isBlank()){
            throw new IllegalArgumentException("UserId cannot be empty");
        }

        return userDAO.findById(userId);
    }

    @Override
    public List<User> getAllUser() {
        return userDAO.findAll();
    }

    @Override
    public void deleteUser(String userId) {
        if (userId == null || userId.isBlank()){
            throw new IllegalArgumentException("UserId cannot be empty");
        }

        userDAO.delete(userId);
    }
}
