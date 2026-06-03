package com.app.common.entity;

import java.io.Serializable;
import java.util.Arrays;

public class ItemImage extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemId;
    private String imagePath;
    private byte[] imageData;
    private int displayOrder;

    public ItemImage() {
    }

    public ItemImage(String itemId, String imagePath, byte[] imageData, int displayOrder) {
        this.itemId = itemId;
        this.imagePath = imagePath;
        this.imageData = imageData;
        this.displayOrder = displayOrder;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Override
    public String toString() {
        return "ItemImage{" +
                "id='" + getId() + '\'' +
                ", itemId='" + itemId + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", imageData length=" + (imageData != null ? imageData.length : 0) +
                ", displayOrder=" + displayOrder +
                '}';
    }
}

