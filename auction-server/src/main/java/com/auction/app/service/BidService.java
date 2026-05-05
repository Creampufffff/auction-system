package com.auction.app.service;

import com.app.common.entity.BidTransaction;

import java.util.List;

public interface BidService {
    void placeBid(BidTransaction bid);

    BidTransaction getBidById(String bidID);

    List<BidTransaction> getAllBids();

    List<BidTransaction> getBidByAuctionId(String auctionId);

    void deleteBid(String bidId);
}
