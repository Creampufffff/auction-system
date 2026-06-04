package com.auction.app.service.impl;

import com.app.common.entity.Auction;
import com.app.common.entity.AutoBid;
import com.app.common.entity.BidTransaction;
import com.app.common.entity.Bidder;
import com.app.common.exception.InvalidBidException;
import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.service.AutoBidService;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;

public class AutoBidServiceImpl implements AutoBidService {
    private final List<AutoBid> autoBids = new ArrayList<>();  // List of all AutoBids
    private final AuctionDAO auctionDAO;
    private final BidDAO bidDAO;
    
    // PriorityQueue sorts AutoBids by max auto amount (highest amount has priority)
    private final PriorityQueue<AutoBid> autoBidQueue;

    public AutoBidServiceImpl(AuctionDAO auctionDAO, BidDAO bidDAO) {
        this.auctionDAO = auctionDAO;
        this.bidDAO = bidDAO;
        
        // Create a PriorityQueue sorted in descending order (highest maxAutoAmount first)
        this.autoBidQueue = new PriorityQueue<>(
                Comparator.comparingDouble(AutoBid::getMaxAutoAmount).reversed()
        );
    }

    @Override
    public void createAutoBid(AutoBid autoBid) {
        // ========== STEP 1: Validate AutoBid ==========
        if (autoBid == null) {
            throw new InvalidBidException("AutoBid cannot be null");
        }
        
        if (autoBid.getAuctionId() == null || autoBid.getAuctionId().isBlank()) {
            throw new InvalidBidException("Auction ID cannot be empty");
        }
        
        if (autoBid.getBidderId() == null || autoBid.getBidderId().isBlank()) {
            throw new InvalidBidException("Bidder ID cannot be empty");
        }
        
        if (autoBid.getMaxAutoAmount() <= 0) {
            throw new InvalidBidException("Max auto amount must be greater than 0");
        }

        // ========== STEP 2: Check auction exists ==========
        Auction auction = auctionDAO.findById(autoBid.getAuctionId());
        if (auction == null) {
            throw new InvalidBidException("Auction not found");
        }

        // ========== STEP 3: Save AutoBid ==========
        autoBids.add(autoBid);       // Save into list
        autoBidQueue.offer(autoBid); // Add into PriorityQueue
    }

    @Override
    public AutoBid getAutoBidById(String autoBidId) {
        return autoBids.stream()
                .filter(ab -> ab.getId().equals(autoBidId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<AutoBid> getAutoBiddsByAuctionId(String auctionId) {
        List<AutoBid> result = new ArrayList<>();
        
        // Get all active AutoBids for this auction
        for (AutoBid ab : autoBids) {
            if (ab.getAuctionId().equals(auctionId) && ab.isActive()) {
                result.add(ab);
            }
        }
        
        // Sort by max auto amount (highest first)
        result.sort(Comparator.comparingDouble(AutoBid::getMaxAutoAmount).reversed());
        return result;
    }

    @Override
    public List<AutoBid> getActiveBidsByBidderId(String bidderId) {
        List<AutoBid> result = new ArrayList<>();
        
        // Get all active AutoBids for this bidder
        for (AutoBid ab : autoBids) {
            if (ab.getBidderId().equals(bidderId) && ab.isActive()) {
                result.add(ab);
            }
        }
        
        return result;
    }

    @Override
    public void cancelAutoBid(String autoBidId) {
        AutoBid autoBid = getAutoBidById(autoBidId);
        if (autoBid != null) {
            autoBid.setActive(false);  // Mark as inactive
            autoBidQueue.remove(autoBid);  // Remove from queue
        }
    }

    @Override
    public synchronized void processAutoBidsForAuction(String auctionId) {
        // Get all active AutoBids for the auction
        List<AutoBid> activeAutoBids = getAutoBiddsByAuctionId(auctionId);

        if (activeAutoBids.isEmpty()) {
            return;  // No AutoBids
        }

        // Lấy thông tin phiên và vật phẩm
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null || auction.getItem() == null) {
            return;
        }

        // ========== BƯỚC 1: Lấy bid hiện tại ==========
        BidTransaction currentHighest = bidDAO.getMaxBidByAuctionId(auctionId);
        double currentPrice = currentHighest != null
                ? currentHighest.getBidAmount()
                : auction.getItem().getStartPrice();  // If no bids yet, use start price
        String leadingBidderId = currentHighest != null ? currentHighest.getBidder().getId() : null;

        double minIncrement = auction.getItem().getMinIncreasement();

        // Keep alternating challengers until nobody except the leader can afford the next bid.
        while (true) {
            double nextBidAmount = currentPrice + minIncrement;
            String currentLeaderId = leadingBidderId;
            AutoBid challenger = activeAutoBids.stream()
                    .filter(AutoBid::isActive)
                    .filter(autoBid -> !autoBid.getBidderId().equals(currentLeaderId))
                    .filter(autoBid -> autoBid.getMaxAutoAmount() >= nextBidAmount)
                    .max(Comparator.comparingDouble(AutoBid::getMaxAutoAmount))
                    .orElse(null);

            if (challenger == null) {
                activeAutoBids.stream()
                        .filter(AutoBid::isActive)
                        .filter(autoBid -> !autoBid.getBidderId().equals(currentLeaderId))
                        .filter(autoBid -> autoBid.getMaxAutoAmount() < nextBidAmount)
                        .forEach(autoBid -> autoBid.setActive(false));
                return;
            }

            try {
                Bidder bidder = new Bidder("auto", "auto", "auto@system.com");
                bidder.setId(challenger.getBidderId());
                BidTransaction autoBidTransaction = new BidTransaction(bidder, auction, nextBidAmount);

                bidDAO.placeBidSafely(autoBidTransaction);
                currentPrice = nextBidAmount;
                leadingBidderId = challenger.getBidderId();
            } catch (Exception e) {
                System.err.println("AutoBid failed for ID: " + challenger.getId() + " - " + e.getMessage());
                challenger.setActive(false);
            }
        }
    }
}

