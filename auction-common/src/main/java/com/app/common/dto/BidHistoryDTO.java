package com.app.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class BidHistoryDTO implements Serializable {
    // Getters and Setters
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

}

