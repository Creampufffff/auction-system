package com.auction.app.service.impl;

import com.app.common.entity.BidTransaction;
import com.app.common.entity.User;
import com.app.common.exception.InsufficientBalanceException;
import com.app.common.exception.InvalidBidException;
import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.repository.UserDAO;
import com.auction.app.repository.impl.UserDAOImpl;
import com.auction.app.service.BidService;

import java.util.List;

public class BidServiceImpl implements BidService {
    private final BidDAO bidDAO;        // DAO to access database
    @SuppressWarnings("unused")
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;      // DAO to check users

    public BidServiceImpl(BidDAO bidDAO, AuctionDAO auctionDAO) {
        this(bidDAO, auctionDAO, new UserDAOImpl());
    }

    public BidServiceImpl(BidDAO bidDAO, AuctionDAO auctionDAO, UserDAO userDAO) {
        this.bidDAO = bidDAO;
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
    }

    @Override
    public void placeBid(BidTransaction bid) {
        if (bid == null) {
            throw new InvalidBidException("Bid cannot be null");
        }

        if (bid.getAuction() == null || bid.getAuction().getId() == null || bid.getAuction().getId().isBlank()) {
            throw new InvalidBidException("Auction ID cannot be empty");
        }

        if (bid.getBidder() == null || bid.getBidder().getId() == null || bid.getBidder().getId().isBlank()) {
            throw new InvalidBidException("Bidder ID cannot be empty");
        }

        if (bid.getBidAmount() <= 0) {
            throw new InvalidBidException("Bid amount must be greater than 0");
        }

        User bidder = userDAO.findById(bid.getBidder().getId());
        if (bidder == null) {
            throw new InvalidBidException("User not found");
        }


        double reusableHeldAmount = bidDAO.getMaxBidByBidder(bid.getAuction().getId(), bidder.getId());
        if (bidder.getBalance() + reusableHeldAmount < bid.getBidAmount()) {
            throw new InsufficientBalanceException("Insufficient funds to place this bid");
        }


        if (!bidDAO.placeBidSafely(bid)) {
            throw new IllegalStateException("Failed to save bid to database");
        }
    }

    @Override
    public BidTransaction getBidById(String bidId) {
        validateId(bidId, "Bid ID");
        return bidDAO.findById(bidId);
    }

    @Override
    public List<BidTransaction> getAllBids() {
        return bidDAO.findAll();
    }

    @Override
    public List<BidTransaction> getBidByAuctionId(String auctionId) {
        validateId(auctionId, "Auction ID");
        // Trả về danh sách bid sắp xếp từ cao đến thấp
        return bidDAO.findByAuctionId(auctionId);
    }

    @Override
    public void deleteBid(String bidId) {
        validateId(bidId, "ID bid");
        if (!bidDAO.delete(bidId)) {
            throw new IllegalArgumentException("Bid not found to delete");
        }
    }

    // Check ID is not null or empty
    private void validateId(String id, String fieldName) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
    }
}
