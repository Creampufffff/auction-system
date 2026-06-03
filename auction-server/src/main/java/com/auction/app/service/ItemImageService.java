package com.auction.app.service;

import com.app.common.entity.ItemImage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface ItemImageService {
    /**
     * Lưu ảnh từ byte array
     */
    ItemImage uploadImage(String itemId, String fileName, byte[] imageData) throws SQLException, IOException;

    /**
     * Lấy tất cả ảnh của item
     */
    List<ItemImage> getImagesByItemId(String itemId) throws SQLException;

    /**
     * Lấy ảnh thumbnail
     */
    ItemImage getThumbnailByItemId(String itemId) throws SQLException;

    /**
     * Xóa ảnh
     */
    void deleteImage(String imageId) throws SQLException;

    /**
     * Xóa tất cả ảnh của item
     */
    void deleteImagesByItemId(String itemId) throws SQLException;

    /**
     * Cập nhật thứ tự hiển thị
     */
    void updateDisplayOrder(String imageId, int order) throws SQLException;
}

