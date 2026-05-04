package src.main.java.com.auction.app.service;

import src.main.java.com.app.common.entity.User;

import java.util.List;

public interface UserService {
    void register(User user);

    User login(String username, String password);

    User getById(String UserId);

    List<User> getAllUser();

    void deleteUser(String userId);
}
