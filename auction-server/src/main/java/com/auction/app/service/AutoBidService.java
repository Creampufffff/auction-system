package com.auction.app.service;

import com.app.common.entity.AutoBid;
import java.util.List;

/**
 * AutoBidService: Manages automatic bidding functionality
 * Allows bidders to set maximum amounts for auto-bidding
 */
public interface AutoBidService {
    /**
     * Create an auto-bid instruction
     * @param autoBid Auto-bid to create
     */
    void createAutoBid(AutoBid autoBid);

    /**
     * Get auto-bid by ID
     * @param autoBidId Auto-bid ID
     * @return AutoBid object
     */
    AutoBid getAutoBidById(String autoBidId);

    /**
     * Get all auto-bids for an auction
     * @param auctionId Auction ID
     * @return List of active auto-bids
     */
    List<AutoBid> getAutoBiddsByAuctionId(String auctionId);

    /**
     * Get all active auto-bids for a bidder
     * @param bidderId Bidder ID
     * @return List of active auto-bids
     */
    List<AutoBid> getActiveBidsByBidderId(String bidderId);

    /**
     * Cancel an auto-bid instruction
     * @param autoBidId Auto-bid ID to cancel
     */
    void cancelAutoBid(String autoBidId);

    /**
     * Process auto-bids for an auction when a new bid is placed
     * @param auctionId Auction ID
     */
    void processAutoBidsForAuction(String auctionId);
}

