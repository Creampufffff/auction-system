package com.auction.app.service.impl;

import com.app.common.entity.ItemImage;
import com.auction.app.repository.ItemImageDAO;
import com.auction.app.service.ItemImageService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class ItemImageServiceImpl implements ItemImageService {
    private final ItemImageDAO itemImageDAO;

    public ItemImageServiceImpl(ItemImageDAO itemImageDAO) {
        this.itemImageDAO = itemImageDAO;
    }

    @Override
    public ItemImage uploadImage(String itemId, String fileName, byte[] imageData) throws SQLException, IOException {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item ID không hợp lệ");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("Dữ liệu ảnh không hợp lệ");
        }

        // Lấy số ảnh hiện tại để gán thứ tự
        List<ItemImage> existingImages = itemImageDAO.getImagesByItemId(itemId);
        int displayOrder = existingImages.size();

        ItemImage image = new ItemImage(itemId, fileName, imageData, displayOrder);
        image.setId(UUID.randomUUID().toString());

        return itemImageDAO.save(image);
    }

    @Override
    public List<ItemImage> getImagesByItemId(String itemId) throws SQLException {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item ID không hợp lệ");
        }
        return itemImageDAO.getImagesByItemId(itemId);
    }

    @Override
    public ItemImage getThumbnailByItemId(String itemId) throws SQLException {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item ID không hợp lệ");
        }
        return itemImageDAO.getThumbnailByItemId(itemId);
    }

    @Override
    public void deleteImage(String imageId) throws SQLException {
        if (imageId == null || imageId.isBlank()) {
            throw new IllegalArgumentException("Image ID không hợp lệ");
        }
        itemImageDAO.delete(imageId);
    }

    @Override
    public void deleteImagesByItemId(String itemId) throws SQLException {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item ID không hợp lệ");
        }
        itemImageDAO.deleteImagesByItemId(itemId);
    }

    @Override
    public void updateDisplayOrder(String imageId, int order) throws SQLException {
        if (imageId == null || imageId.isBlank()) {
            throw new IllegalArgumentException("Image ID không hợp lệ");
        }
        if (order < 0) {
            throw new IllegalArgumentException("Display order phải >= 0");
        }
        itemImageDAO.updateDisplayOrder(imageId, order);
    }
}

