package com.app.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
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

}

