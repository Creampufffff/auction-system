package src.main.java.com.auction.app.service.impl;
import src.main.java.com.app.common.entity.Auction;
import src.main.java.com.app.common.enums.Status;
import src.main.java.com.auction.app.repository.AuctionDAO;
import src.main.java.com.auction.app.service.AuctionService;

import java.util.ArrayList;
import java.util.List;

public class AuctionServiceImpl implements AuctionService {
    private final AuctionDAO auctionDAO;

    public AuctionServiceImpl(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }

    @Override
    public void saveAuction(Auction auction) {
        if (auction == null){
            throw new IllegalArgumentException("Auction cannot be null!");
        }

        auctionDAO.save(auction);
    }

    @Override
    public void startAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);

        if (auction == null || auction.getAuctionStatus() != Status.OPEN){
            throw new IllegalArgumentException("Auction not found or ended");
        }

        auction.setAuctionStatus(Status.RUNNING);
        auctionDAO.save(auction);
    }

    @Override
    public void endAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);

        if (auction == null){
            throw new IllegalArgumentException("Auction not found");
        }

        auction.setAuctionStatus(Status.FINISHED);
        auctionDAO.save(auction);
    }

    @Override
    public Auction getAuctionById(String id) {
        return auctionDAO.findById(id);
    }

    @Override
    public List<Auction> getAllAuction() {
        return auctionDAO.findAll();
    }

    @Override
    public List<Auction> getActiveAuctions() {
//        List<Auction> result = new ArrayList<>();
//
//        for (Auction auction : auctionDAO.findAll()){
//            if (auction.getAuctionStatus() == Status.RUNNING){
//                result.add(auction);
//            }
//        }

        return auctionDAO.findActiveAuctions();
    }
}
