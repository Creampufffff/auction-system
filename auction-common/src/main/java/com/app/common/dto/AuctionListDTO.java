package com.app.common.dto;

import java.io.Serializable;

import com.app.common.enums.Status;
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

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Status getAuctionStatus() {
        return auctionStatus;
    }

    public void setAuctionStatus(Status auctionStatus) {
        this.auctionStatus = auctionStatus;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWarranty() {
        return warranty;
    }

    public void setWarranty(String warranty) {
        this.warranty = warranty;
    }
}
