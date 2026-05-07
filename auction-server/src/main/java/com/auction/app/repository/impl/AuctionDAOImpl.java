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
            throw new IllegalStateException("Không tải được đấu giá ", e);
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
            throw new IllegalStateException("Không cập nhập được giá cho phieen: " + auctionId, e);
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
            throw new IllegalStateException("Không thể tìm được phiên đấu " + id, e);
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
            throw new IllegalStateException("Không load được đáu giá", e);
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
            throw new IllegalStateException("Không thể lưu đấu giá: " + entity.getId(), e);
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
            throw new IllegalStateException("Không xóa được phiên đấu: " + id, e);
        }
    }

    private Auction mapAuction(ResultSet resultSet) throws SQLException {
        Item item = itemDAO.findById(resultSet.getString("item_id"));
        Auction auction = new Auction(item);
        auction.setId(resultSet.getString("id"));
        auction.setAuctionStatus(Status.valueOf(resultSet.getString("status")));
        return auction;
    }
}
