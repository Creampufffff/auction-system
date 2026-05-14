package com.app.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class AutoBidDTO implements Serializable {
    // Getters and Setters
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

}

