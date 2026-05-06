package com.auction.app.service.impl;

import com.app.common.entity.BidTransaction;
import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.service.BidService;

import java.util.List;

public class BidServiceImpl implements BidService {
    private final BidDAO bidDAO;

    public BidServiceImpl(BidDAO bidDAO, AuctionDAO auctionDAO) {
        this.bidDAO = bidDAO;
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

        if (!bidDAO.placeBidSafely(bid)) {
            throw new IllegalStateException("Cannot save bid");
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
