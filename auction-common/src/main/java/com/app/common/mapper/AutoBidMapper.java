package com.app.common.mapper;

import com.app.common.dto.*;
import com.app.common.entity.*;

/**
 * AutoBidMapper - Chuyển đổi giữa AutoBid Entity và các DTO liên quan
 */
public class AutoBidMapper {

    /**
     * Chuyển AutoBid Entity thành AutoBidDTO
     */
    public static AutoBidDTO toDTO(AutoBid autoBid, String itemName) {
        if (autoBid == null) return null;
        
        return new AutoBidDTO(
            autoBid.getId(),
            autoBid.getAuctionId(),
            itemName,
            autoBid.getBidderId(),
            autoBid.getMaxAutoAmount(),
            autoBid.isActive()
        );
    }

    /**
     * Chuyển SetAutoBidRequestDTO thành AutoBid Entity
     */
    public static AutoBid toEntity(SetAutoBidRequestDTO dto) {
        if (dto == null) return null;
        
        return new AutoBid(
            dto.getAuctionId(),
            dto.getBidderId(),
            dto.getMaxAutoAmount()
        );
    }
}

