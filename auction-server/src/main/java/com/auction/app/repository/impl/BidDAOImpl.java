package com.auction.app.repository.impl;

import com.app.common.entity.Auction;
import com.app.common.entity.Bidder;
import com.app.common.entity.BidTransaction;
import com.app.common.entity.User;
import com.auction.app.config.DatabaseConfig;
import com.auction.app.repository.BidDAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAOImpl implements BidDAO {
    private final AuctionDAOImpl auctionDAO = new AuctionDAOImpl();
    private final UserDAOImpl userDAO = new UserDAOImpl();

    @Override
    public List<BidTransaction> findByAuctionId(String auctionId) {
        String sql = """
                SELECT id, auction_id, bidder_id, bid_amount
                FROM bid_transactions
                WHERE auction_id = ?
                ORDER BY bid_amount DESC, created_at DESC
                """;
        List<BidTransaction> bids = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auctionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    bids.add(mapBid(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load bids for auction: " + auctionId, e);
        }

        return bids;
    }

    @Override
    public double getMaxBidByBidder(String auctionId, String bidderId) {
        String sql = "SELECT MAX(bid_amount) AS max_bid FROM bid_transactions WHERE auction_id = ? AND bidder_id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auctionId);
            statement.setString(2, bidderId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("max_bid");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot get max bid for bidder: " + bidderId, e);
        }

        return 0;
    }

    @Override
    public BidTransaction getMaxBidByAuctionId(String auctionId) {
        String sql = """
                SELECT id, auction_id, bidder_id, bid_amount
                FROM bid_transactions
                WHERE auction_id = ?
                ORDER BY bid_amount DESC, created_at DESC
                LIMIT 1
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auctionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapBid(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot get max bid for auction: " + auctionId, e);
        }

        return null;
    }

    @Override
    public BidTransaction findById(String id) {
        String sql = "SELECT id, auction_id, bidder_id, bid_amount FROM bid_transactions WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapBid(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot find bid by id: " + id, e);
        }

        return null;
    }

    @Override
    public List<BidTransaction> findAll() {
        String sql = "SELECT id, auction_id, bidder_id, bid_amount FROM bid_transactions ORDER BY created_at DESC";
        List<BidTransaction> bids = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                bids.add(mapBid(resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load bids", e);
        }

        return bids;
    }

    @Override
    public boolean save(BidTransaction entity) {
        String sql = """
                INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    auction_id = VALUES(auction_id),
                    bidder_id = VALUES(bidder_id),
                    bid_amount = VALUES(bid_amount)
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entity.getId());
            statement.setString(2, entity.getAuction().getId());
            statement.setString(3, entity.getBidder().getId());
            statement.setDouble(4, entity.getBidAmount());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save bid: " + entity.getId(), e);
        }
    }

    @Override
    public boolean placeBidSafely(BidTransaction bid) {
        // Lock auction record to serialize concurrent bid operations.
        String lockAuctionSql = """
                SELECT a.status,
                       i.start_price,
                       i.min_increment,
                       i.highest_current_price
                FROM auctions a
                JOIN items i ON i.id = a.item_id
                WHERE a.id = ?
                FOR UPDATE
                """;
        String bidderSql = "SELECT balance FROM bidder WHERE id = ? FOR UPDATE";
        String maxBidSql = "SELECT MAX(bid_amount) AS max_bid FROM bid_transactions WHERE auction_id = ?";
        String insertBidSql = """
                INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount)
                VALUES (?, ?, ?, ?)
                """;
        String updateAuctionSql = """
                UPDATE auctions a
                JOIN items i ON i.id = a.item_id
                SET i.highest_current_price = ?,
                    a.last_bidder_id = ?,
                    a.current_version = a.current_version + 1
                WHERE a.id = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);

            try {
                // Toàn bộ các bước kiểm tra + ghi bid + cập nhật giá phải cùng 1 transaction.
                LockedAuction lockedAuction = lockAuction(connection, lockAuctionSql, bid.getAuction().getId());
                validateBidder(connection, bidderSql, bid.getBidder().getId(), bid.getBidAmount());
                validateBidAmount(connection, maxBidSql, bid, lockedAuction);
                insertBid(connection, insertBidSql, bid);
                updateAuctionPrice(connection, updateAuctionSql, bid);

                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot place bid safely: " + bid.getId(), e);
        }
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM bid_transactions WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot delete bid: " + id, e);
        }
    }

    private BidTransaction mapBid(ResultSet resultSet) throws SQLException {
        Auction auction = auctionDAO.findById(resultSet.getString("auction_id"));
        User user = userDAO.findById(resultSet.getString("bidder_id"));

        if (!(user instanceof Bidder)) {
            throw new IllegalStateException("Bidder id does not belong to a bidder: " + resultSet.getString("bidder_id"));
        }

        BidTransaction bid = new BidTransaction((Bidder) user, auction, resultSet.getDouble("bid_amount"));
        bid.setId(resultSet.getString("id"));
        return bid;
    }

    private LockedAuction lockAuction(Connection connection, String sql, String auctionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auctionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Auction not found");
                }

                String status = resultSet.getString("status");
                if (!"RUNNING".equals(status)) {
                    throw new IllegalArgumentException("Auction is not active");
                }

                double startPrice = resultSet.getDouble("start_price");
                double minIncrement = resultSet.getDouble("min_increment");
                double highestCurrentPrice = resultSet.getDouble("highest_current_price");

                return new LockedAuction(startPrice, minIncrement, highestCurrentPrice);
            }
        }
    }

    private void validateBidder(Connection connection, String sql, String bidderId, double bidAmount) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bidderId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Bidder not found");
                }
                if (resultSet.getDouble("balance") < bidAmount) {
                    throw new IllegalArgumentException("Insufficient funds to place this bid");
                }
            }
        }
    }

    private void validateBidAmount(Connection connection, String sql, BidTransaction bid, LockedAuction auction) throws SQLException {
        double currentPrice = auction.highestCurrentPrice > 0 ? auction.highestCurrentPrice : auction.startPrice;
        double minimumAcceptedBid = currentPrice + auction.minIncrement;

        if (bid.getBidAmount() < minimumAcceptedBid) {
            throw new IllegalArgumentException("Bid must be at least " + minimumAcceptedBid);
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bid.getAuction().getId());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    double maxBid = resultSet.getDouble("max_bid");
                    if (!resultSet.wasNull() && bid.getBidAmount() <= maxBid) {
                        throw new IllegalArgumentException("Bid must be higher than current highest bid");
                    }
                }
            }
        }
    }

    private void insertBid(Connection connection, String sql, BidTransaction bid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bid.getId());
            statement.setString(2, bid.getAuction().getId());
            statement.setString(3, bid.getBidder().getId());
            statement.setDouble(4, bid.getBidAmount());
            statement.executeUpdate();
        }
    }

    private void updateAuctionPrice(Connection connection, String sql, BidTransaction bid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, bid.getBidAmount());
            statement.setString(2, bid.getBidder().getId());
            statement.setString(3, bid.getAuction().getId());

            if (statement.executeUpdate() == 0) {
                throw new IllegalStateException("Cannot update auction current price");
            }
        }
    }

    private static class LockedAuction {
        private final double startPrice;
        private final double minIncrement;
        private final double highestCurrentPrice;

        private LockedAuction(double startPrice, double minIncrement, double highestCurrentPrice) {
            this.startPrice = startPrice;
            this.minIncrement = minIncrement;
            this.highestCurrentPrice = highestCurrentPrice;
        }
    }
}
