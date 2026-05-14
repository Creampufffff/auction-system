package com.app.common.dto;

import java.io.Serializable;

public class AuctionDetailResponseDTO implements Serializable {
    private String auctionId;
    private String itemName;
    private String description;
    private String condition;
    private String warranty;
    private double startPrice;
    private double currentPrice;
    private double minIncrement;
    private String status;
    private String sellerUsername;
    private String highestBidderUsername;
    private String startDateTime;
    private String endDateTime;
    private long remainingSeconds;

    public AuctionDetailResponseDTO() {}

    public AuctionDetailResponseDTO(String auctionId, String itemName, String description,
                                   String condition, String warranty, double startPrice,
                                   double currentPrice, double minIncrement, String status,
                                   String sellerUsername, String highestBidderUsername,
                                   String startDateTime, String endDateTime, long remainingSeconds) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.description = description;
        this.condition = condition;
        this.warranty = warranty;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.minIncrement = minIncrement;
        this.status = status;
        this.sellerUsername = sellerUsername;
        this.highestBidderUsername = highestBidderUsername;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.remainingSeconds = remainingSeconds;
    }

    // Getters and Setters
    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

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

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getMinIncrement() { return minIncrement; }
    public void setMinIncrement(double minIncrement) { this.minIncrement = minIncrement; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }

    public String getHighestBidderUsername() { return highestBidderUsername; }
    public void setHighestBidderUsername(String highestBidderUsername) { this.highestBidderUsername = highestBidderUsername; }

    public String getStartDateTime() { return startDateTime; }
    public void setStartDateTime(String startDateTime) { this.startDateTime = startDateTime; }

    public String getEndDateTime() { return endDateTime; }
    public void setEndDateTime(String endDateTime) { this.endDateTime = endDateTime; }

    public long getRemainingSeconds() { return remainingSeconds; }
    public void setRemainingSeconds(long remainingSeconds) { this.remainingSeconds = remainingSeconds; }
}

