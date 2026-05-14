package com.auction.app.repository;

import com.app.common.entity.BidTransaction;

import java.util.List;

public interface BidDAO extends BaseDAO<BidTransaction> {
    // Get full bid history of an auction (for charting)
    List<BidTransaction> findByAuctionId(String auctionId);

    // Get the maximum bid amount a bidder placed in an auction
    double getMaxBidByBidder(String auctionId, String bidderId);

    // Get the highest bid of an auction
    BidTransaction getMaxBidByAuctionId(String auctionId);

    // Place a bid inside a transaction to avoid lost updates when many clients bid concurrently
    boolean placeBidSafely(BidTransaction bid);
}
