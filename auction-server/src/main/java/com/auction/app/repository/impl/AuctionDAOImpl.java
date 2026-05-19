package com.auction.app.repository.impl;

import com.app.common.entity.Auction;
import com.app.common.entity.Item;
import com.app.common.enums.Status;
import com.auction.app.config.DatabaseConfig;
import com.auction.app.repository.AuctionDAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAOImpl implements AuctionDAO {
    private final ItemDAOImpl itemDAO = new ItemDAOImpl();

    @Override
    public List<Auction> findActiveAuctions() {
        String sql = "SELECT id, item_id, status FROM auctions WHERE status IN (?, ?) ORDER BY id";
        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, Status.OPEN.name());
            statement.setString(2, Status.RUNNING.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auctions.add(mapAuction(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load auctions", e);
        }

        return auctions;
    }

    @Override
    public List<Auction> findRunningAuctions() {
        String sql = "SELECT id, item_id, status FROM auctions WHERE status = ? ORDER BY id";
        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, Status.RUNNING.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auctions.add(mapAuction(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load running auctions", e);
        }

        return auctions;
    }

    @Override
    public boolean updateCurrentPrice(String auctionId, double newPrice, String lastBidderId, int currentVersion) {
        String sql = """
                UPDATE auctions a
                JOIN items i ON i.id = a.item_id
                SET i.highest_current_price = ?,
                    a.last_bidder_id = ?,
                    a.current_version = a.current_version + 1
                WHERE a.id = ? AND a.current_version = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, newPrice);
            statement.setString(2, lastBidderId);
            statement.setString(3, auctionId);
            statement.setInt(4, currentVersion);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update price for auction: " + auctionId, e);
        }
    }

    @Override
    public boolean settleAndFinishAuction(String auctionId) {
        String lockAuctionSql = """
                SELECT a.status, i.seller_id
                FROM auctions a
                JOIN items i ON i.id = a.item_id
                WHERE a.id = ?
                FOR UPDATE
                """;
        String highestBidSql = """
                SELECT bidder_id, bid_amount
                FROM bid_transactions
                WHERE auction_id = ?
                ORDER BY bid_amount DESC, created_at DESC
                LIMIT 1
                """;
        String lockBidderSql = "SELECT balance FROM bidder WHERE id = ? FOR UPDATE";
        String lockSellerSql = "SELECT balance FROM seller WHERE id = ? FOR UPDATE";
        String updateBidderBalanceSql = "UPDATE bidder SET balance = ? WHERE id = ?";
        String updateSellerBalanceSql = "UPDATE seller SET balance = ? WHERE id = ?";
        String finishAuctionSql = "UPDATE auctions SET status = ? WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);

            try {
                String sellerId;
                try (PreparedStatement statement = connection.prepareStatement(lockAuctionSql)) {
                    statement.setString(1, auctionId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new IllegalArgumentException("Auction not found");
                        }

                        String status = resultSet.getString("status");
                        if (Status.FINISHED.name().equals(status)) {
                            connection.commit();
                            return true;
                        }
                        if (Status.CANCELED.name().equals(status)) {
                            throw new IllegalStateException("Cannot finish canceled auction");
                        }
                        sellerId = resultSet.getString("seller_id");
                    }
                }

                String bidderId = null;
                double amount = 0;
                try (PreparedStatement statement = connection.prepareStatement(highestBidSql)) {
                    statement.setString(1, auctionId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            bidderId = resultSet.getString("bidder_id");
                            amount = resultSet.getDouble("bid_amount");
                        }
                    }
                }

                if (bidderId != null) {
                    double bidderBalance = lockUserBalance(connection, lockBidderSql, bidderId, "Winner not found");
                    double sellerBalance = lockUserBalance(connection, lockSellerSql, sellerId, "Seller not found");

                    if (bidderBalance < amount) {
                        throw new IllegalStateException("Winner has insufficient balance");
                    }

                    updateUserBalance(connection, updateBidderBalanceSql, bidderId, bidderBalance - amount);
                    updateUserBalance(connection, updateSellerBalanceSql, sellerId, sellerBalance + amount);
                }

                try (PreparedStatement statement = connection.prepareStatement(finishAuctionSql)) {
                    statement.setString(1, Status.FINISHED.name());
                    statement.setString(2, auctionId);
                    if (statement.executeUpdate() == 0) {
                        throw new IllegalStateException("Failed to finish auction");
                    }
                }

                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to settle auction: " + auctionId, e);
        }
    }

    @Override
    public Auction findById(String id) {
        String sql = "SELECT id, item_id, status FROM auctions WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAuction(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find auction " + id, e);
        }

        return null;
    }

    @Override
    public List<Auction> findAll() {
        String sql = "SELECT id, item_id, status FROM auctions ORDER BY id";
        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                auctions.add(mapAuction(resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load auctions", e);
        }

        return auctions;
    }

    @Override
    public boolean save(Auction entity) {
        String sql = """
                INSERT INTO auctions (id, item_id, status)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    item_id = VALUES(item_id),
                    status = VALUES(status)
                """;

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                itemDAO.save(entity.getItem(), connection);
                statement.setString(1, entity.getId());
                statement.setString(2, entity.getItem().getId());
                statement.setString(3, entity.getAuctionStatus().name());

                boolean saved = statement.executeUpdate() > 0;
                connection.commit();
                return saved;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save auction: " + entity.getId(), e);
        }
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM auctions WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete auction: " + id, e);
        }
    }

    private Auction mapAuction(ResultSet resultSet) throws SQLException {
        Item item = itemDAO.findById(resultSet.getString("item_id"));
        Auction auction = new Auction(item);
        auction.setId(resultSet.getString("id"));
        auction.setAuctionStatus(Status.valueOf(resultSet.getString("status")));
        return auction;
    }

    private double lockUserBalance(Connection connection, String sql, String userId, String notFoundMessage) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException(notFoundMessage);
                }
                return resultSet.getDouble("balance");
            }
        }
    }

    private void updateUserBalance(Connection connection, String sql, String userId, double balance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, balance);
            statement.setString(2, userId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalStateException("Failed to update user balance");
            }
        }
    }
}
