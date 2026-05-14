package com.app.common.mapper;

import com.app.common.dto.*;
import com.app.common.entity.*;

public class BidMapper {

    public static BidTransaction toEntity(PlaceBidRequestDTO dto, Bidder bidder, Auction auction) {
        if (dto == null || bidder == null || auction == null) return null;
        
        return new BidTransaction(bidder, auction, dto.getBidAmount());
    }
}

