package com.app.common.entity;

/**
 * AutoBid: Represents an automatic bidding instruction
 * A bidder can set a maximum amount, and the system auto-bids incrementally
 * up to that amount when other bids are placed.
 */
public class AutoBid extends BaseEntity {
    private String auctionId;
    private String bidderId;
    private double maxAutoAmount;  // Maximum amount willing to bid automatically
    private boolean isActive;

    public AutoBid(String auctionId, String bidderId, double maxAutoAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxAutoAmount = maxAutoAmount;
        this.isActive = true;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public double getMaxAutoAmount() {
        return maxAutoAmount;
    }

    public void setMaxAutoAmount(double maxAutoAmount) {
        if (maxAutoAmount <= 0) {
            throw new IllegalArgumentException("Auto-bid amount must be positive");
        }
        this.maxAutoAmount = maxAutoAmount;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}

