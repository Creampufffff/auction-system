package com.app.common.dto;

import java.io.Serializable;

public class AutoBidDTO implements Serializable {
    private String autoBidId;
    private String auctionId;
    private String itemName;
    private String bidderId;
    private double maxAutoAmount;
    private boolean isActive;

    public AutoBidDTO() {}

    public AutoBidDTO(String autoBidId, String auctionId, String itemName, 
                     String bidderId, double maxAutoAmount, boolean isActive) {
        this.autoBidId = autoBidId;
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.bidderId = bidderId;
        this.maxAutoAmount = maxAutoAmount;
        this.isActive = isActive;
    }

    // Getters and Setters
    public String getAutoBidId() { return autoBidId; }
    public void setAutoBidId(String autoBidId) { this.autoBidId = autoBidId; }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }

    public double getMaxAutoAmount() { return maxAutoAmount; }
    public void setMaxAutoAmount(double maxAutoAmount) { this.maxAutoAmount = maxAutoAmount; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}

