package com.app.common.dto;

import java.io.Serializable;

import com.app.common.enums.Status;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuctionListDTO implements Serializable {
    private String auctionId;
    private String itemId;
    private String name;
    private double currentPrice;
    private Status auctionStatus;
    private String condition;
    private String description;
    private String warranty;

    public AuctionListDTO() {
    }

    public AuctionListDTO(
            String auctionId,
            String itemId,
            String name,
            double currentPrice,
            Status auctionStatus,
            String condition,
            String description,
            String warranty
    ) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.name = name;
        this.currentPrice = currentPrice;
        this.auctionStatus = auctionStatus;
        this.condition = condition;
        this.description = description;
        this.warranty = warranty;
    }

}
