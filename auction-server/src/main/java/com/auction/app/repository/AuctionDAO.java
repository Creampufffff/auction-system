package com.auction.app.repository;

import com.app.common.entity.Auction;

import java.util.List;

public interface AuctionDAO extends BaseDAO<Auction> {
    List<Auction> findActiveAuctions();

    List<Auction> findRunningAuctions();

    List<Auction> findBySellerId(String sellerId);

    List<Auction> findWonByBidderId(String bidderId);

    boolean updateCurrentPrice(String auctionId, double newPrice, String lastBidderId, int currentVersion);

    boolean settleAndFinishAuction(String auctionId);

    /**
     * Update the underlying item's end_date for the given auction.
     */
    boolean updateItemEndDate(String auctionId, String newEndDate);
}
