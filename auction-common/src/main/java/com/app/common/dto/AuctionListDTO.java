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
    private String itemType;
    private String name;
    private double currentPrice;
    private Status auctionStatus;
    private String condition;
    private String description;
    private String warranty;
    private String startDateTime;
    private String endDateTime;
    private byte[] imageBlob;

    public AuctionListDTO() {
    }

    public AuctionListDTO(
            String auctionId,
            String itemId,
            String itemType,
            String name,
            double currentPrice,
            Status auctionStatus,
            String condition,
            String description,
            String warranty
    ) {
        this(auctionId, itemId, itemType, name, currentPrice, auctionStatus, condition, description, warranty, null, null);
    }

    public AuctionListDTO(
            String auctionId,
            String itemId,
            String itemType,
            String name,
            double currentPrice,
            Status auctionStatus,
            String condition,
            String description,
            String warranty,
            String startDateTime,
            String endDateTime
    ) {
        this(auctionId, itemId, itemType, name, currentPrice, auctionStatus, condition, description, warranty,
                startDateTime, endDateTime, null);
    }

    public AuctionListDTO(
            String auctionId,
            String itemId,
            String itemType,
            String name,
            double currentPrice,
            Status auctionStatus,
            String condition,
            String description,
            String warranty,
            String startDateTime,
            String endDateTime,
            byte[] imageBlob
    ) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.itemType = itemType;
        this.name = name;
        this.currentPrice = currentPrice;
        this.auctionStatus = auctionStatus;
        this.condition = condition;
        this.description = description;
        this.warranty = warranty;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.imageBlob = imageBlob;
    }

}
