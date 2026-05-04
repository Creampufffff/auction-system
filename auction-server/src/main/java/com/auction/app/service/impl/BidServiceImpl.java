package src.main.java.com.auction.app.service.impl;

import src.main.java.com.app.common.entity.Auction;
import src.main.java.com.app.common.entity.BidTransaction;
import src.main.java.com.app.common.enums.Status;
import src.main.java.com.auction.app.repository.AuctionDAO;
import src.main.java.com.auction.app.repository.BidDAO;
import src.main.java.com.auction.app.service.BidService;

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

        Auction auction = auctionDAO.findById(bid.getId());

        if (auction == null) {
            throw new IllegalArgumentException("Auction not found");
        }

        if (auction.getAuctionStatus() != Status.RUNNING) {
            throw new IllegalArgumentException("Auction is not active");
        }

        BidTransaction highestBid = bidDAO.getMaxBidByAuctionId(bid.getId());

        if (highestBid != null && bid.getBidAmount() <= highestBid.getBidAmount()) {
            throw new IllegalArgumentException("Bid must be higher than current highest bid");
        }

        bidDAO.save(bid);
    }

    @Override
    public BidTransaction getBidById(String bidId){
        return bidDAO.findById(bidId);
    }

    @Override
    public List<BidTransaction> getAllBids() {
        return bidDAO.findAll();
    }

    @Override
    public List<BidTransaction> getBidByAuctionId(String auctionId) {
        return bidDAO.findByAuctionId(auctionId);
    }

    @Override
    public void deleteBid(String bidId) {
        bidDAO.delete(bidId);
    }
}
