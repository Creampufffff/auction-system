package com.auction.app.controller;

import com.app.common.entity.BidTransaction;
import com.auction.app.service.BidService;

import java.util.List;


public class BidController {
    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    public void placeBid(BidTransaction bid) {
        bidService.placeBid(bid);
    }

    public BidTransaction getBid(String bidId) {
        return bidService.getBidById(bidId);
    }

    public List<BidTransaction> getAllBids() {
        return bidService.getAllBids();
    }

    public List<BidTransaction> getAuctionBidHistory(String auctionId) {
        return bidService.getBidByAuctionId(auctionId);
    }

    public void deleteBid(String bidId) {
        bidService.deleteBid(bidId);
    }
}


