package com.auction.app.service.impl;

import com.app.common.entity.Art;
import com.app.common.entity.Auction;
import com.app.common.entity.BidTransaction;
import com.app.common.entity.Bidder;
import com.app.common.entity.Seller;
import com.app.common.entity.User;
import com.app.common.enums.Status;
import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.repository.UserDAO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionServiceImplTest {
    @Test
    void saveAuctionDefaultsStatusToOpen() {
        FakeAuctionDAO auctionDAO = new FakeAuctionDAO();
        Auction auction = new Auction(sampleArt());
        auction.setAuctionStatus(null);

        new AuctionServiceImpl(auctionDAO).saveAuction(auction);

        assertEquals(Status.OPEN, auction.getAuctionStatus());
        assertEquals(auction, auctionDAO.savedAuction);
    }

    @Test
    void startAuctionMovesOpenAuctionToRunning() {
        FakeAuctionDAO auctionDAO = new FakeAuctionDAO();
        Auction auction = new Auction(sampleArt());
        auctionDAO.savedAuction = auction;

        new AuctionServiceImpl(auctionDAO).startAuction(auction.getId());

        assertEquals(Status.RUNNING, auction.getAuctionStatus());
    }

    @Test
    void startAuctionRejectsFinishedAuction() {
        FakeAuctionDAO auctionDAO = new FakeAuctionDAO();
        Auction auction = new Auction(sampleArt());
        auction.setAuctionStatus(Status.FINISHED);
        auctionDAO.savedAuction = auction;

        assertThrows(com.app.common.exception.AuctionClosedException.class, () -> new AuctionServiceImpl(auctionDAO).startAuction(auction.getId()));
    }

    @Test
    void endAuctionSettlesWinnerAndSellerBalance() {
        FakeAuctionDAO auctionDAO = new FakeAuctionDAO();
        FakeBidDAO bidDAO = new FakeBidDAO();
        FakeUserDAO userDAO = new FakeUserDAO();

        Seller seller = new Seller("seller", "pass", "seller@example.com");
        Bidder bidder = new Bidder("bidder", "pass", "bidder@example.com");
        bidder.deposit(500);
        userDAO.save(seller);
        userDAO.save(bidder);

        Art art = sampleArt();
        art.setSellerId(seller.getId());
        Auction auction = new Auction(art);
        auction.setAuctionStatus(Status.RUNNING);
        auctionDAO.savedAuction = auction;

        bidDAO.maxBid = new BidTransaction(bidder, auction, 200);
        new AuctionServiceImpl(auctionDAO, bidDAO, userDAO).endAuction(auction.getId());

        assertEquals(Status.FINISHED, auction.getAuctionStatus());
        assertEquals(300, userDAO.findById(bidder.getId()).getBalance());
        assertEquals(200, userDAO.findById(seller.getId()).getBalance());
    }

    private static Art sampleArt() {
        return new Art("description", "Painting", "2026-01-01", "2026-01-02", 100, 10, "Author");
    }

    private static class FakeAuctionDAO implements AuctionDAO {
        private Auction savedAuction;

        @Override
        public List<Auction> findActiveAuctions() {
            return savedAuction == null ? List.of() : List.of(savedAuction);
        }

        @Override
        public boolean updateCurrentPrice(String auctionId, double newPrice, String lastBidderId, int currentVersion) {
            return true;
        }

        @Override
        public Auction findById(String id) {
            return savedAuction != null && savedAuction.getId().equals(id) ? savedAuction : null;
        }

        @Override
        public List<Auction> findAll() {
            return savedAuction == null ? new ArrayList<>() : new ArrayList<>(List.of(savedAuction));
        }

        @Override
        public boolean save(Auction entity) {
            savedAuction = entity;
            return true;
        }

        @Override
        public boolean delete(String id) {
            if (savedAuction != null && savedAuction.getId().equals(id)) {
                savedAuction = null;
                return true;
            }
            return false;
        }
    }

    private static class FakeBidDAO implements BidDAO {
        private BidTransaction maxBid;

        @Override
        public List<BidTransaction> findByAuctionId(String auctionId) {
            return List.of();
        }

        @Override
        public double getMaxBidByBidder(String auctionId, String bidderId) {
            return 0;
        }

        @Override
        public BidTransaction getMaxBidByAuctionId(String auctionId) {
            return maxBid;
        }

        @Override
        public boolean placeBidSafely(BidTransaction bid) {
            return true;
        }

        @Override
        public BidTransaction findById(String id) {
            return null;
        }

        @Override
        public List<BidTransaction> findAll() {
            return List.of();
        }

        @Override
        public boolean save(BidTransaction entity) {
            return true;
        }

        @Override
        public boolean delete(String id) {
            return false;
        }
    }

    private static class FakeUserDAO implements UserDAO {
        private final List<User> users = new ArrayList<>();

        @Override
        public User findByUsername(String username) {
            return users.stream().filter(user -> user.getUsername().equals(username)).findFirst().orElse(null);
        }

        @Override
        public User findById(String id) {
            return users.stream().filter(user -> user.getId().equals(id)).findFirst().orElse(null);
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(users);
        }

        @Override
        public boolean save(User entity) {
            users.removeIf(user -> user.getId().equals(entity.getId()));
            users.add(entity);
            return true;
        }

        @Override
        public boolean delete(String id) {
            return users.removeIf(user -> user.getId().equals(id));
        }
    }
}
