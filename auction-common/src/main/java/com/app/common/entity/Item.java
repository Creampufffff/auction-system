package com.app.common.entity;

abstract public class Item extends BaseEntity {

    private String name;
    private String description;
    private double startPrice;
    private double minIncreasement;
    private String endDateString;
    private String startDateString;
    private double highestCurrentPrice;
    private String sellerId;
    private byte[] imageBlob;

    public Item(String description, String name, String startDateString, String endDateString, double startPrice, double minIncreasement) {
        this.description = description;
        this.name = name;
        this.startDateString = startDateString;
        this.endDateString = endDateString;
        this.startPrice = startPrice;
        this.minIncreasement = minIncreasement;
    }

    public double getHighestCurrentPrice() {
        return highestCurrentPrice;
    }

    public void setHighestCurrentPrice(double highestCurrentPrice) {
        this.highestCurrentPrice = highestCurrentPrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEndDateString() {
        return endDateString;
    }

    public void setEndDateString(String endDateString) {
        this.endDateString = endDateString;
    }

    public double getMinIncreasement() {
        return minIncreasement;
    }

    public String getName() {
        return name;
    }

    public String getStartDateString() {
        return startDateString;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public byte[] getImageBlob() {
        return imageBlob;
    }

    public void setImageBlob(byte[] imageBlob) {
        this.imageBlob = imageBlob;
    }
}

