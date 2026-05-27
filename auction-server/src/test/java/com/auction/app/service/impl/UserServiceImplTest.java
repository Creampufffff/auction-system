package com.auction.app.service.impl;

import com.app.common.entity.Bidder;
import com.app.common.entity.User;
import com.auction.app.repository.UserDAO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceImplTest {
    @Test
    void registerRejectsDuplicateUsername() {
        FakeUserDAO userDAO = new FakeUserDAO();
        Bidder existingUser = new Bidder("alice", "pass", "alice@example.com");
        userDAO.save(existingUser);

        UserServiceImpl service = new UserServiceImpl(userDAO);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.register(new Bidder("alice", "other", "alice2@example.com"))
        );
        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void loginReturnsMatchingUser() {
        FakeUserDAO userDAO = new FakeUserDAO();
        Bidder user = new Bidder("alice", "pass", "alice@example.com");
        userDAO.save(user);

        UserServiceImpl service = new UserServiceImpl(userDAO);

        assertSame(user, service.login("alice", "pass"));
    }

    @Test
    void loginRejectsWrongPassword() {
        FakeUserDAO userDAO = new FakeUserDAO();
        userDAO.save(new Bidder("alice", "pass", "alice@example.com"));

        UserServiceImpl service = new UserServiceImpl(userDAO);

        assertThrows(com.app.common.exception.UserAuthException.class, () -> service.login("alice", "wrong"));
    }

    private static class FakeUserDAO implements UserDAO {
        private final List<User> users = new ArrayList<>();

        @Override
        public User findByUsername(String username) {
            return users.stream()
                    .filter(user -> user.getUsername().equals(username))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public User findById(String id) {
            return users.stream()
                    .filter(user -> user.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<User> findAll() {
            return users;
        }

        @Override
        public boolean save(User entity) {
            users.add(entity);
            return true;
        }

        @Override
        public boolean updateBalance(User user) {
            return save(user);
        }

        @Override
        public boolean delete(String id) {
            return users.removeIf(user -> user.getId().equals(id));
        }
    }
}
