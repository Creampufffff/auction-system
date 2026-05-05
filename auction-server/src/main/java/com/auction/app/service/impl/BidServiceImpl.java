package com.auction.app.service.impl;

import com.app.common.entity.Auction;
import com.app.common.entity.BidTransaction;
import com.app.common.enums.Status;
import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.service.BidService;

import java.util.List;

public class BidServiceImpl implements BidService {
    private final BidDAO bidDAO;
    private final AuctionDAO auctionDAO;

    public BidServiceImpl(BidDAO bidDAO, AuctionDAO auctionDAO) {
        this.bidDAO = bidDAO;
        this.auctionDAO = auctionDAO;
    }

    @Override
    public void placeBid(BidTransaction bid) {
        if (bid == null) {
            throw new IllegalArgumentException("Bid cannot be null");
        }

        if (bid.getAuction() == null || bid.getAuction().getId() == null || bid.getAuction().getId().isBlank()) {
            throw new IllegalArgumentException("Auction cannot be empty");
        }

        if (bid.getBidder() == null || bid.getBidder().getId() == null || bid.getBidder().getId().isBlank()) {
            throw new IllegalArgumentException("Bidder cannot be empty");
        }

        if (bid.getBidAmount() <= 0) {
            throw new IllegalArgumentException("Bid amount must be greater than 0");
        }

        Auction auction = auctionDAO.findById(bid.getAuction().getId());

        if (auction == null) {
            throw new IllegalArgumentException("Auction not found");
        }

        if (auction.getAuctionStatus() != Status.RUNNING) {
            throw new IllegalArgumentException("Auction is not active");
        }

        if (auction.getItem() == null) {
            throw new IllegalStateException("Auction item not found");
        }

        double currentPrice = auction.getItem().getHighestCurrentPrice();
        if (currentPrice <= 0) {
            currentPrice = auction.getItem().getStartPrice();
        }

        double minimumAcceptedBid = currentPrice + auction.getItem().getMinIncreasement();
        if (bid.getBidAmount() < minimumAcceptedBid) {
            throw new IllegalArgumentException("Bid must be at least " + minimumAcceptedBid);
        }

        BidTransaction highestBid = bidDAO.getMaxBidByAuctionId(auction.getId());

        if (highestBid != null && bid.getBidAmount() <= highestBid.getBidAmount()) {
            throw new IllegalArgumentException("Bid must be higher than current highest bid");
        }

        bid.setAuction(auction);

        if (!bidDAO.save(bid)) {
            throw new IllegalStateException("Cannot save bid");
        }

        auction.getItem().setHighestCurrentPrice(bid.getBidAmount());
        if (!auctionDAO.save(auction)) {
            throw new IllegalStateException("Cannot update auction current price");
        }
    }

    @Override
    public BidTransaction getBidById(String bidId) {
        validateId(bidId, "Bid id");
        return bidDAO.findById(bidId);
    }

    @Override
    public List<BidTransaction> getAllBids() {
        return bidDAO.findAll();
    }

    @Override
    public List<BidTransaction> getBidByAuctionId(String auctionId) {
        validateId(auctionId, "Auction id");
        return bidDAO.findByAuctionId(auctionId);
    }

    @Override
    public void deleteBid(String bidId) {
        validateId(bidId, "Bid id");
        if (!bidDAO.delete(bidId)) {
            throw new IllegalArgumentException("Bid not found");
        }
    }

    private void validateId(String id, String fieldName) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
    }
}
