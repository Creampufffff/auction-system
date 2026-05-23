package com.auction.app.service;

import com.app.common.entity.AutoBid;
import java.util.List;

public interface AutoBidService {
    void createAutoBid(AutoBid autoBid);

    AutoBid getAutoBidById(String autoBidId);

    List<AutoBid> getAutoBiddsByAuctionId(String auctionId);

    List<AutoBid> getActiveBidsByBidderId(String bidderId);

    void cancelAutoBid(String autoBidId);

    void processAutoBidsForAuction(String auctionId);
}

