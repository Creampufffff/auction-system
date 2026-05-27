package com.auction.app.repository;

import com.app.common.entity.ItemImage;

import java.sql.SQLException;
import java.util.List;

public interface ItemImageDAO {
    /**
     * Lưu ảnh mới
     */
    ItemImage save(ItemImage entity) throws SQLException;

    /**
     * Lấy ảnh theo ID
     */
    ItemImage findById(String id) throws SQLException;

    /**
     * Lấy tất cả ảnh của một item, sắp xếp theo thứ tự hiển thị
     */
    List<ItemImage> getImagesByItemId(String itemId) throws SQLException;

    /**
     * Lấy ảnh đầu tiên (thumbnail) của item
     */
    ItemImage getThumbnailByItemId(String itemId) throws SQLException;

    /**
     * Xóa tất cả ảnh của một item
     */
    void deleteImagesByItemId(String itemId) throws SQLException;

    /**
     * Cập nhật thứ tự hiển thị của ảnh
     */
    void updateDisplayOrder(String imageId, int order) throws SQLException;

    /**
     * Cập nhật ảnh
     */
    void update(ItemImage entity) throws SQLException;

    /**
     * Xóa ảnh
     */
    void delete(String id) throws SQLException;

    /**
     * Lấy tất cả ảnh
     */
    List<ItemImage> findAll() throws SQLException;
}

