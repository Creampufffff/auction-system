package com.auction.app.repository.impl;

import com.app.common.entity.User;
import com.auction.app.server.config.DatabaseConfig;
import com.auction.app.repository.BaseDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

public class UserDAOImpl implements BaseDAO<User> {


    @Override
    public User findById(String id) {
        return null;
    }

    @Override
    public List<User> findAll() {
        return List.of();
    }

    @Override
    public boolean save(User entity) {
        return false;
    }

    @Override
    public boolean delete(String id) {
        return false;
    }
}