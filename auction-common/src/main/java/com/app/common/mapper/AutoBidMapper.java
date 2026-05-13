package com.app.common.mapper;

import com.app.common.dto.*;
import com.app.common.entity.*;

public class AutoBidMapper {

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

    public static AutoBid toEntity(SetAutoBidRequestDTO dto) {
        if (dto == null) return null;
        
        return new AutoBid(
            dto.getAuctionId(),
            dto.getBidderId(),
            dto.getMaxAutoAmount()
        );
    }
}

