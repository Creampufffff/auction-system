package com.auction.app.controller;

import com.app.common.entity.AutoBid;
import com.auction.app.service.AutoBidService;

import java.util.List;

public class AutoBidController {
    private final AutoBidService autoBidService;

    public AutoBidController(AutoBidService autoBidService) {
        this.autoBidService = autoBidService;
    }

    public void setAutoBid(AutoBid autoBid) {
        autoBidService.createAutoBid(autoBid);
    }

    public AutoBid getAutoBid(String autoBidId) {
        return autoBidService.getAutoBidById(autoBidId);
    }

    public List<AutoBid> getAuctionAutoBids(String auctionId) {
        return autoBidService.getAutoBiddsByAuctionId(auctionId);
    }

    public List<AutoBid> getBidderAutoBids(String bidderId) {
        return autoBidService.getActiveBidsByBidderId(bidderId);
    }

    public void cancelAutoBid(String autoBidId) {
        autoBidService.cancelAutoBid(autoBidId);
    }

    public void processAutoBids(String auctionId) {
        autoBidService.processAutoBidsForAuction(auctionId);
    }
}


