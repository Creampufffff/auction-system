package com.auction.app.service.impl;

import com.app.common.entity.Auction;
import com.app.common.entity.BidTransaction;
import com.app.common.entity.User;
import com.app.common.exception.AuctionClosedException;
import com.app.common.exception.AuctionNotFoundException;
import com.app.common.enums.Status;
import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.repository.UserDAO;
import com.auction.app.repository.impl.BidDAOImpl;
import com.auction.app.repository.impl.UserDAOImpl;
import com.auction.app.service.AuctionService;

import java.util.List;

public class AuctionServiceImpl implements AuctionService {
    private final AuctionDAO auctionDAO;
    private final BidDAO bidDAO;
    private final UserDAO userDAO;

    public AuctionServiceImpl(AuctionDAO auctionDAO) {
        this(auctionDAO, new BidDAOImpl(), new UserDAOImpl());
    }

    public AuctionServiceImpl(AuctionDAO auctionDAO, BidDAO bidDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.bidDAO = bidDAO;
        this.userDAO = userDAO;
    }

    @Override
    public void saveAuction(Auction auction) {
        if (auction == null) {
            throw new IllegalArgumentException("Auction cannot be null!");
        }

        if (auction.getItem() == null) {
            throw new IllegalArgumentException("Auction item cannot be null");
        }

        if (auction.getAuctionStatus() == null) {
            auction.setAuctionStatus(Status.OPEN);
        }

        if (!auctionDAO.save(auction)) {
            throw new IllegalStateException("Failed to save auction");
        }
    }

    @Override
    public void startAuction(String auctionId) {
        validateId(auctionId);

        Auction auction = getAuctionById(auctionId);

        if (auction == null) {
            throw new AuctionNotFoundException("Auction not found");
        }

        if (auction.getAuctionStatus() != Status.OPEN) {
            throw new AuctionClosedException("Can only start auction when status is OPEN");
        }

        auction.setAuctionStatus(Status.RUNNING);

        if (!auctionDAO.save(auction)) {
            throw new IllegalStateException("Failed to start auction");
        }
    }

    @Override
    public void endAuction(String auctionId) {
        validateId(auctionId);

        Auction auction = getAuctionById(auctionId);

        if (auction == null) {
            throw new AuctionNotFoundException("Auction not found");
        }

        if (auction.getAuctionStatus() == Status.FINISHED) {
            return;
        }

        if (!auctionDAO.settleAndFinishAuction(auctionId)) {
            throw new IllegalStateException("Failed to end auction");
        }
    }

    @Override
    public Auction getAuctionById(String id) {
        validateId(id);
        return auctionDAO.findById(id);
    }

    @Override
    public List<Auction> getAllAuction() {
        return auctionDAO.findAll();
    }

    @Override
    public List<Auction> getActiveAuctions() {
        return auctionDAO.findActiveAuctions();
    }

    private void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Auction ID cannot be empty");
        }
    }

    private void settleWinningBid(Auction auction) {

        BidTransaction highestBid = bidDAO.getMaxBidByAuctionId(auction.getId());

        if (highestBid == null) {
            return;
        }

        User winner = userDAO.findById(highestBid.getBidder().getId());
        if (winner == null) {
            throw new IllegalStateException("Winner not found");
        }

        winner.withdraw(highestBid.getBidAmount());

        String sellerId = auction.getItem().getSellerId();
        User seller = userDAO.findById(sellerId);
        if (seller == null) {
            throw new IllegalStateException("Seller not found");
        }

        seller.deposit(highestBid.getBidAmount());

        if (!userDAO.save(winner)) {
            throw new IllegalStateException("Failed to update winner's account");
        }
        if (!userDAO.save(seller)) {
            throw new IllegalStateException("Failed to update seller's account");
        }
    }
}
