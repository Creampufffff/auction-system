package com.auction.app.service.impl;

import com.app.common.entity.Art;
import com.app.common.entity.Auction;
import com.app.common.entity.BidTransaction;
import com.app.common.entity.Bidder;
import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.repository.UserDAO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BidServiceImplTest {
    @Test
    void placeBidDelegatesValidBidToDao() {
        FakeBidDAO bidDAO = new FakeBidDAO(true);
        BidTransaction bid = sampleBid(150);
        FakeUserDAO userDAO = new FakeUserDAO();
        userDAO.save(bid.getBidder());
        BidServiceImpl service = new BidServiceImpl(bidDAO, new NoopAuctionDAO(), userDAO);

        service.placeBid(bid);

        assertSame(bid, bidDAO.savedBid);
    }

    @Test
    void placeBidRejectsNonPositiveAmount() {
        BidServiceImpl service = new BidServiceImpl(new FakeBidDAO(true), new NoopAuctionDAO(), new FakeUserDAO());

        assertThrows(com.app.common.exception.InvalidBidException.class, () -> service.placeBid(sampleBid(0)));
    }

    @Test
    void placeBidThrowsWhenDaoCannotSave() {
        BidTransaction bid = sampleBid(150);
        FakeUserDAO userDAO = new FakeUserDAO();
        userDAO.save(bid.getBidder());
        BidServiceImpl service = new BidServiceImpl(new FakeBidDAO(false), new NoopAuctionDAO(), userDAO);

        assertThrows(IllegalStateException.class, () -> service.placeBid(bid));
    }

    private static BidTransaction sampleBid(double amount) {
        Art item = new Art("description", "Painting", "2026-01-01", "2026-01-02", 100, 10, "Author");
        Auction auction = new Auction(item);
        Bidder bidder = new Bidder("alice", "pass", "alice@example.com");
        bidder.deposit(1000);
        return new BidTransaction(bidder, auction, amount);
    }

    private static class FakeBidDAO implements BidDAO {
        private final boolean saveResult;
        private BidTransaction savedBid;

        private FakeBidDAO(boolean saveResult) {
            this.saveResult = saveResult;
        }

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
            return null;
        }

        @Override
        public boolean placeBidSafely(BidTransaction bid) {
            savedBid = bid;
            return saveResult;
        }

        @Override
        public BidTransaction findById(String id) {
            return savedBid != null && savedBid.getId().equals(id) ? savedBid : null;
        }

        @Override
        public List<BidTransaction> findAll() {
            return savedBid == null ? new ArrayList<>() : new ArrayList<>(List.of(savedBid));
        }

        @Override
        public boolean save(BidTransaction entity) {
            savedBid = entity;
            return saveResult;
        }

        @Override
        public boolean delete(String id) {
            boolean exists = savedBid != null && savedBid.getId().equals(id);
            if (exists) {
                savedBid = null;
            }
            return exists;
        }
    }

    private static class NoopAuctionDAO implements AuctionDAO {
        @Override
        public List<Auction> findActiveAuctions() {
            return List.of();
        }

        @Override
        public boolean updateCurrentPrice(String auctionId, double newPrice, String lastBidderId, int currentVersion) {
            return true;
        }

        @Override
        public Auction findById(String id) {
            return null;
        }

        @Override
        public List<Auction> findAll() {
            return List.of();
        }

        @Override
        public boolean save(Auction entity) {
            return true;
        }

        @Override
        public boolean delete(String id) {
            return true;
        }
    }

    private static class FakeUserDAO implements UserDAO {
        private final List<com.app.common.entity.User> users = new ArrayList<>();

        @Override
        public com.app.common.entity.User findByUsername(String username) {
            return users.stream()
                    .filter(user -> user.getUsername().equals(username))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public com.app.common.entity.User findById(String id) {
            return users.stream()
                    .filter(user -> user.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<com.app.common.entity.User> findAll() {
            return new ArrayList<>(users);
        }

        @Override
        public boolean save(com.app.common.entity.User entity) {
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
