package com.app.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
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

}
