package com.app.common.dto;

import java.io.Serializable;

public class PlaceBidRequestDTO implements Serializable {
    private String auctionId;
    private String bidderId;
    private double bidAmount;

    public PlaceBidRequestDTO() {
    }

    public PlaceBidRequestDTO(String auctionId, String bidderId, double bidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
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

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }
}
