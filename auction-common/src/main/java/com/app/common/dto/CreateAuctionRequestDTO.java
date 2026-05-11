package com.app.common.dto;

import java.io.Serializable;

public class CreateAuctionRequestDTO implements Serializable {
    private String itemName;
    private String description;
    private String condition;
    private String warranty;
    private double startPrice;
    private double minIncrement;
    private String startDateTime;
    private String endDateTime;
    private String sellerId;
    private String itemType; // "ART", "ELECTRONICS", "VEHICLE"

    public CreateAuctionRequestDTO() {}

    public CreateAuctionRequestDTO(String itemName, String description, String condition,
                                  String warranty, double startPrice, double minIncrement,
                                  String startDateTime, String endDateTime, String sellerId,
                                  String itemType) {
        this.itemName = itemName;
        this.description = description;
        this.condition = condition;
        this.warranty = warranty;
        this.startPrice = startPrice;
        this.minIncrement = minIncrement;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.sellerId = sellerId;
        this.itemType = itemType;
    }

    // Getters and Setters
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getWarranty() { return warranty; }
    public void setWarranty(String warranty) { this.warranty = warranty; }

    public double getStartPrice() { return startPrice; }
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }

    public double getMinIncrement() { return minIncrement; }
    public void setMinIncrement(double minIncrement) { this.minIncrement = minIncrement; }

    public String getStartDateTime() { return startDateTime; }
    public void setStartDateTime(String startDateTime) { this.startDateTime = startDateTime; }

    public String getEndDateTime() { return endDateTime; }
    public void setEndDateTime(String endDateTime) { this.endDateTime = endDateTime; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
}

