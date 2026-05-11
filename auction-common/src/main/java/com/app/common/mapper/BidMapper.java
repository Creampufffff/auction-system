package com.app.common.mapper;

import com.app.common.dto.*;
import com.app.common.entity.*;

/**
 * BidMapper - Chuyển đổi giữa Bid Entity và các DTO liên quan
 */
public class BidMapper {

    /**
     * Chuyển PlaceBidRequestDTO thành BidTransaction Entity
     */
    public static BidTransaction toEntity(PlaceBidRequestDTO dto, Bidder bidder, Auction auction) {
        if (dto == null || bidder == null || auction == null) return null;
        
        return new BidTransaction(bidder, auction, dto.getBidAmount());
    }
}

