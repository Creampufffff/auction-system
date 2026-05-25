package com.app.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class CreateAuctionRequestDTO implements Serializable {
    // Getters and Setters
    private String itemName;
    private String description;
    private String condition;
    private String warranty;
    private double startPrice;
    private double minIncrement;
    private String startDateTime;
    private String endDateTime;
    private String sellerId;
    private String itemType; // "ART", "ELECTRONICS", "VEHICLE"

    public CreateAuctionRequestDTO() {}

    public CreateAuctionRequestDTO(String itemName, String description, String condition,
                                  String warranty, double startPrice, double minIncrement,
                                  String startDateTime, String endDateTime, String sellerId,
                                  String itemType) {
        this.itemName = itemName;
        this.description = description;
        this.condition = condition; // author for art
        this.warranty = warranty; // warranty months for electronics, brand for vehicle
        this.startPrice = startPrice;
        this.minIncrement = minIncrement;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.sellerId = sellerId;
        this.itemType = itemType;
    }

}

