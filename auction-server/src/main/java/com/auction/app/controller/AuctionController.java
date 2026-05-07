package com.auction.app.controller;

import com.app.common.entity.Auction;
import com.auction.app.service.AuctionService;

import java.util.List;

public class AuctionController {
    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void createAuction(Auction auction) {
        auctionService.saveAuction(auction);
    }

    public void startAuction(String auctionId) {
        auctionService.startAuction(auctionId);
    }

    public void endAuction(String auctionId) {
        auctionService.endAuction(auctionId);
    }

    public Auction getAuction(String auctionId) {
        return auctionService.getAuctionById(auctionId);
    }

    public List<Auction> getAllAuctions() {
        return auctionService.getAllAuction();
    }

    public List<Auction> getActiveAuctions() {
        return auctionService.getActiveAuctions();
    }
}


