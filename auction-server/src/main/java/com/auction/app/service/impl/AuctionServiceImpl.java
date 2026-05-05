package com.auction.app.service.impl;

import com.app.common.entity.Auction;
import com.app.common.enums.Status;
import com.auction.app.repository.AuctionDAO;
import com.auction.app.service.AuctionService;

import java.util.List;

public class AuctionServiceImpl implements AuctionService {
    private final AuctionDAO auctionDAO;

    public AuctionServiceImpl(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
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
            throw new IllegalStateException("Cannot save auction");
        }
    }

    @Override
    public void startAuction(String auctionId) {
        validateId(auctionId, "Auction id");
        Auction auction = getAuctionById(auctionId);

        if (auction == null) {
            throw new IllegalArgumentException("Auction not found");
        }

        if (auction.getAuctionStatus() != Status.OPEN) {
            throw new IllegalArgumentException("Only OPEN auctions can be started");
        }

        auction.setAuctionStatus(Status.RUNNING);
        if (!auctionDAO.save(auction)) {
            throw new IllegalStateException("Cannot start auction");
        }
    }

    @Override
    public void endAuction(String auctionId) {
        validateId(auctionId, "Auction id");
        Auction auction = getAuctionById(auctionId);

        if (auction == null) {
            throw new IllegalArgumentException("Auction not found");
        }

        if (auction.getAuctionStatus() == Status.FINISHED) {
            return;
        }

        auction.setAuctionStatus(Status.FINISHED);
        if (!auctionDAO.save(auction)) {
            throw new IllegalStateException("Cannot end auction");
        }
    }

    @Override
    public Auction getAuctionById(String id) {
        validateId(id, "Auction id");
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

    private void validateId(String id, String fieldName) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
    }
}
