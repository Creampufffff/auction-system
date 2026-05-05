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
            throw new IllegalStateException("Cannot load bids for auction: " + auctionId, e);
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
}
