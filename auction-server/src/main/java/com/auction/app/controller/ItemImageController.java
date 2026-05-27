package com.auction.app.controller;

import com.app.common.entity.ItemImage;
import com.auction.app.service.ItemImageService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ItemImageController {
    private final ItemImageService itemImageService;

    public ItemImageController(ItemImageService itemImageService) {
        this.itemImageService = itemImageService;
    }

    /**
     * Upload ảnh mới cho item
     * Format: UPLOAD_IMAGE itemId|fileName|base64EncodedData
     */
    public String uploadImage(String itemId, String fileName, byte[] imageData) {
        try {
            ItemImage saved = itemImageService.uploadImage(itemId, fileName, imageData);
            return "OK|IMAGE_UPLOADED|" + saved.getId() + "|" + fileName;
        } catch (SQLException | IOException e) {
            return "ERR|UPLOAD_FAILED|" + e.getMessage();
        }
    }

    /**
     * Lấy danh sách ảnh của item
     * Format: GET_IMAGES itemId
     */
    public String getImages(String itemId) {
        try {
            List<ItemImage> images = itemImageService.getImagesByItemId(itemId);
            if (images.isEmpty()) {
                return "OK|IMAGES|EMPTY";
            }

            String imageList = images.stream()
                    .map(img -> img.getId() + "," + img.getImagePath() + "," + img.getDisplayOrder())
                    .collect(Collectors.joining("|"));

            return "OK|IMAGES|" + imageList;
        } catch (SQLException e) {
            return "ERR|GET_IMAGES_FAILED|" + e.getMessage();
        }
    }

    /**
     * Lấy ảnh thumbnail (ảnh đầu tiên)
     * Format: GET_THUMBNAIL itemId
     */
    public String getThumbnail(String itemId) {
        try {
            ItemImage thumbnail = itemImageService.getThumbnailByItemId(itemId);
            if (thumbnail == null) {
                return "OK|THUMBNAIL|NONE";
            }
            return "OK|THUMBNAIL|" + thumbnail.getId() + "|" + thumbnail.getImagePath();
        } catch (SQLException e) {
            return "ERR|GET_THUMBNAIL_FAILED|" + e.getMessage();
        }
    }

    /**
     * Xóa ảnh
     * Format: DELETE_IMAGE imageId
     */
    public String deleteImage(String imageId) {
        try {
            itemImageService.deleteImage(imageId);
            return "OK|IMAGE_DELETED";
        } catch (SQLException e) {
            return "ERR|DELETE_IMAGE_FAILED|" + e.getMessage();
        }
    }

    /**
     * Xóa tất cả ảnh của item
     * Format: DELETE_IMAGES itemId
     */
    public String deleteImages(String itemId) {
        try {
            itemImageService.deleteImagesByItemId(itemId);
            return "OK|IMAGES_DELETED";
        } catch (SQLException e) {
            return "ERR|DELETE_IMAGES_FAILED|" + e.getMessage();
        }
    }

    /**
     * Cập nhật thứ tự ảnh
     * Format: REORDER_IMAGE imageId|order
     */
    public String reorderImage(String imageId, int order) {
        try {
            itemImageService.updateDisplayOrder(imageId, order);
            return "OK|IMAGE_REORDERED";
        } catch (SQLException e) {
            return "ERR|REORDER_FAILED|" + e.getMessage();
        }
    }
}

