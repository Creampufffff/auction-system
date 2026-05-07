package com.app.common.dto;

import java.io.Serializable;

public class PlaceBidResponseDTO implements Serializable {
    private boolean success;
    private String message;
    private String bidId;
    private String auctionId;
    private double bidAmount;

    public PlaceBidResponseDTO() {
    }

    public PlaceBidResponseDTO(boolean success, String message, String bidId, String auctionId, double bidAmount) {
        this.success = success;
        this.message = message;
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBidId() {
        return bidId;
    }

    public void setBidId(String bidId) {
        this.bidId = bidId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }
}
