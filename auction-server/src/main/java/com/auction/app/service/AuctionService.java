package com.auction.app.service;

import com.app.common.entity.Auction;

import java.util.List;

public interface AuctionService {
    void saveAuction(Auction auction);

    void startAuction(String auctionId);

    void endAuction(String auctionId);

    Auction getAuctionById(String id);

    List<Auction> getAllAuction();

    List<Auction> getActiveAuctions();

    List<Auction> getAuctionsBySellerId(String sellerId);

    List<Auction> getWonAuctionsByBidderId(String bidderId);

    void updateAuction(Auction auction);

    void deleteAuction(String auctionId);

    /**
     * If the auction is running and its remaining time is less than or equal to thresholdSeconds,
     * extend the auction end time by extensionSeconds.
     * Returns true if the auction end time was updated, false otherwise.
     */
    boolean extendIfEndingSoon(String auctionId, long thresholdSeconds, long extensionSeconds);
}
