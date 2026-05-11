package com.app.common.dto;

import java.io.Serializable;

public class BidHistoryDTO implements Serializable {
    private String bidId;
    private String auctionId;
    private String bidderUsername;
    private double bidAmount;
    private String bidTime;

    public BidHistoryDTO() {}

    public BidHistoryDTO(String bidId, String auctionId, String bidderUsername, 
                        double bidAmount, String bidTime) {
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    // Getters and Setters
    public String getBidId() { return bidId; }
    public void setBidId(String bidId) { this.bidId = bidId; }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public String getBidderUsername() { return bidderUsername; }
    public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }

    public double getBidAmount() { return bidAmount; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }

    public String getBidTime() { return bidTime; }
    public void setBidTime(String bidTime) { this.bidTime = bidTime; }
}

