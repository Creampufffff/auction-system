package com.app.common.dto;

import java.io.Serializable;

public class SetAutoBidRequestDTO implements Serializable {
    private String auctionId;
    private String bidderId;
    private double maxAutoAmount;

    public SetAutoBidRequestDTO() {}

    public SetAutoBidRequestDTO(String auctionId, String bidderId, double maxAutoAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxAutoAmount = maxAutoAmount;
    }

    // Getters and Setters
    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }

    public double getMaxAutoAmount() { return maxAutoAmount; }
    public void setMaxAutoAmount(double maxAutoAmount) { this.maxAutoAmount = maxAutoAmount; }
}

