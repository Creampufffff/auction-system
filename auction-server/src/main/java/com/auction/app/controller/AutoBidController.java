package com.auction.app.controller;

import com.app.common.dto.*;
import com.app.common.entity.AutoBid;
import com.app.common.mapper.AutoBidMapper;
import com.auction.app.service.AutoBidService;

public class AutoBidController {
    private final AutoBidService autoBidService;

    public AutoBidController(AutoBidService autoBidService) {
        this.autoBidService = autoBidService;
    }

    // ✅ Dùng DTO Request/Response
    public ApiResponseDTO setAutoBid(SetAutoBidRequestDTO request) {
        try {
            if (request.getMaxAutoAmount() <= 0) {
                return new ApiResponseDTO(false, "Auto-bid amount must be greater than 0");
            }

            AutoBid autoBid = AutoBidMapper.toEntity(request);
            autoBidService.createAutoBid(autoBid);

            return new ApiResponseDTO(true, "Auto-bid set successfully. ID: " + autoBid.getId());
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Error setting auto-bid: " + e.getMessage());
        }
    }

    // ✅ Dùng DTO Response
    public ApiResponseDTO cancelAutoBid(String autoBidId) {
        try {
            autoBidService.cancelAutoBid(autoBidId);
            return new ApiResponseDTO(true, "Auto-bid canceled successfully");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Error canceling auto-bid: " + e.getMessage());
        }
    }

    // ✅ Dùng DTO Response
    public ApiResponseDTO processAutoBids(String auctionId) {
        try {
            autoBidService.processAutoBidsForAuction(auctionId);
            return new ApiResponseDTO(true, "Auto-bid processed successfully");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Error processing auto-bid: " + e.getMessage());
        }
    }
}


