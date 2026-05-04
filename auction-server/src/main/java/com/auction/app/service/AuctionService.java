package src.main.java.com.auction.app.service;

import src.main.java.com.app.common.entity.Auction;

import java.util.List;

public interface AuctionService {
    void saveAuction(Auction auction);

    void startAuction(String auctionId);

    void endAuction(String auctionId);

    Auction getAuctionById(String id);

    List<Auction> getAllAuction();

    List<Auction> getActiveAuctions();
}
