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
    private String itemType;
    private String itemName;
    private String bidderUsername;
    private double bidAmount;
    private String bidTime;

    public BidHistoryDTO() {}

    public BidHistoryDTO(String bidId, String auctionId, String itemType, String itemName, String bidderUsername,
                        double bidAmount, String bidTime) {
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.itemType = itemType;
        this.itemName = itemName;
        this.bidderUsername = bidderUsername;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

}

