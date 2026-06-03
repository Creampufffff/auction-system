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
    public List<Auction> findBySellerId(String sellerId) {
        String sql = """
                SELECT a.id, a.item_id, a.status
                FROM auctions a
                JOIN items i ON i.id = a.item_id
                WHERE i.seller_id = ?
                ORDER BY a.created_at DESC
                """;
        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sellerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auctions.add(mapAuction(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load auctions for seller: " + sellerId, e);
        }

        return auctions;
    }

    @Override
    public List<Auction> findWonByBidderId(String bidderId) {
        String sql = """
                SELECT a.id, a.item_id, a.status
                FROM auctions a
                WHERE a.status = ?
                  AND a.last_bidder_id = ?
                ORDER BY a.updated_at DESC
                """;
        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, Status.FINISHED.name());
            statement.setString(2, bidderId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auctions.add(mapAuction(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load won items for bidder: " + bidderId, e);
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
                SELECT a.status, a.last_bidder_id, i.seller_id, i.highest_current_price
                FROM auctions a
                JOIN items i ON i.id = a.item_id
                WHERE a.id = ?
                FOR UPDATE
                """;
        String lockBidderSql = "SELECT balance, held_balance FROM bidder WHERE id = ? FOR UPDATE";
        String lockSellerSql = "SELECT balance FROM seller WHERE id = ? FOR UPDATE";
        String updateBidderSql = "UPDATE bidder SET balance = ?, held_balance = ? WHERE id = ?";
        String updateSellerBalanceSql = "UPDATE seller SET balance = ? WHERE id = ?";
        String finishAuctionSql = "UPDATE auctions SET status = ? WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);

            try {
                String sellerId;
                String bidderId;
                double amount;
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
                        bidderId = resultSet.getString("last_bidder_id");
                        amount = resultSet.getDouble("highest_current_price");
                    }
                }

                if (bidderId != null && !bidderId.isBlank() && amount > 0) {
                    double bidderBalance;
                    double bidderHeldBalance;
                    try (PreparedStatement statement = connection.prepareStatement(lockBidderSql)) {
                        statement.setString(1, bidderId);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            if (!resultSet.next()) {
                                throw new IllegalStateException("Winner not found");
                            }
                            bidderBalance = resultSet.getDouble("balance");
                            bidderHeldBalance = resultSet.getDouble("held_balance");
                        }
                    }

                    double sellerBalance = lockUserBalance(connection, lockSellerSql, sellerId, "Seller not found");

                    if (bidderHeldBalance < amount) {
                        throw new IllegalStateException("Winner has insufficient held balance");
                    }

                    updateUserBalance(connection, updateBidderSql, bidderId, bidderBalance, bidderHeldBalance - amount);
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
    public boolean updateItemEndDate(String auctionId, String newEndDate) {
        String sql = "UPDATE items i JOIN auctions a ON a.item_id = i.id SET i.end_date = ? WHERE a.id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newEndDate);
            statement.setString(2, auctionId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update auction end date for: " + auctionId, e);
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

    private void updateUserBalance(Connection connection, String sql, String userId, double balance, double heldBalance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, balance);
            statement.setDouble(2, heldBalance);
            statement.setString(3, userId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalStateException("Failed to update user balance");
            }
        }
    }
}
