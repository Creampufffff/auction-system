package com.auction.app.repository;

import src.main.java.com.app.common.entity.User;

public interface UserDAO extends BaseDAO<User> {
    User findByUsername(String username);
}
