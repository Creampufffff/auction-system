package com.app.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceBidResponseDTO {
    private boolean success;
    private String message;
    private String bidId;
    private String auctionId;
    private double bidAmount;
}
