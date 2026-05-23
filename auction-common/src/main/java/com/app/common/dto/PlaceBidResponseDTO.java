package com.app.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class PlaceBidResponseDTO implements Serializable {
    private boolean success;
    private String message;
    private String bidId;
    private String auctionId;
    private double bidAmount;
    private boolean auctionExtended;
    private String newEndDate;

    public PlaceBidResponseDTO() {
    }

    public PlaceBidResponseDTO(boolean success, String message, String bidId, String auctionId, double bidAmount) {
        this.success = success;
        this.message = message;
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
    }

    public PlaceBidResponseDTO(boolean success, String message, String bidId, String auctionId, double bidAmount, boolean auctionExtended, String newEndDate) {
        this.success = success;
        this.message = message;
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.auctionExtended = auctionExtended;
        this.newEndDate = newEndDate;
    }

}
