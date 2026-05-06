package com.auction.app.repository;

import com.app.common.entity.BaseEntity;

import java.util.List;
import java.util.Optional;

public interface BaseDAO<T> {
    // Lấy một đối tượng theo ID (UUID dạng String)
    T findById(String id);

    // Lấy tất cả bản ghi
    List<T> findAll();

    // Thêm mới hoặc cập nhật
    boolean save(T entity);

    // Xóa theo ID
    boolean delete(String id);
}