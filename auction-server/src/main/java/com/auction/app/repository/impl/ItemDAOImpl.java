package com.auction.app.repository.impl;

import com.app.common.entity.Art;
import com.app.common.entity.Electronics;
import com.app.common.entity.Item;
import com.app.common.entity.Vehicle;
import com.auction.app.config.DatabaseConfig;
import com.auction.app.repository.ItemDAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAOImpl implements ItemDAO {
    private static final String TYPE_ART = "ART";
    private static final String TYPE_ELECTRONICS = "ELECTRONICS";
    private static final String TYPE_VEHICLE = "VEHICLE";

    @Override
    public Item findById(String id) {
        String sql = """
                SELECT id, type, name, description, start_date, end_date, start_price,
                       min_increment, highest_current_price, author, warranty_months, brand, seller_id
                FROM items
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapItem(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot find item by id: " + id, e);
        }

        return null;
    }

    @Override
    public List<Item> findAll() {
        String sql = """
                SELECT id, type, name, description, start_date, end_date, start_price,
                       min_increment, highest_current_price, author, warranty_months, brand, seller_id
                FROM items
                ORDER BY name
                """;
        List<Item> items = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                items.add(mapItem(resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load items", e);
        }

        return items;
    }

    @Override
    public boolean save(Item entity) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            return save(entity, connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save item: " + entity.getId(), e);
        }
    }

    boolean save(Item entity, Connection connection) throws SQLException {
        String sql = """
                INSERT INTO items (
                    id, type, name, description, start_date, end_date, start_price,
                    min_increment, highest_current_price, author, warranty_months, brand, seller_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    type = VALUES(type),
                    name = VALUES(name),
                    description = VALUES(description),
                    start_date = VALUES(start_date),
                    end_date = VALUES(end_date),
                    start_price = VALUES(start_price),
                    min_increment = VALUES(min_increment),
                    highest_current_price = VALUES(highest_current_price),
                    author = VALUES(author),
                    warranty_months = VALUES(warranty_months),
                    brand = VALUES(brand),
                    seller_id = VALUES(seller_id)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entity.getId());
            statement.setString(2, resolveType(entity));
            statement.setString(3, entity.getName());
            statement.setString(4, entity.getDescription());
            statement.setString(5, entity.getStartDateString());
            statement.setString(6, entity.getEndDateString());
            statement.setDouble(7, entity.getStartPrice());
            statement.setDouble(8, entity.getMinIncreasement());
            statement.setDouble(9, entity.getHighestCurrentPrice());
            statement.setString(10, entity instanceof Art ? ((Art) entity).getAuthor() : null);

            if (entity instanceof Electronics) {
                statement.setInt(11, ((Electronics) entity).getWarrantyMonths());
            } else {
                statement.setNull(11, Types.INTEGER);
            }

            statement.setString(12, entity instanceof Vehicle ? ((Vehicle) entity).getBrand() : null);
            statement.setString(13, entity.getSellerId());

            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM items WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot delete item: " + id, e);
        }
    }

    private Item mapItem(ResultSet resultSet) throws SQLException {
        String type = resultSet.getString("type");
        Item item;

        if (TYPE_ELECTRONICS.equals(type)) {
            item = new Electronics(
                    resultSet.getString("description"),
                    resultSet.getString("name"),
                    resultSet.getString("start_date"),
                    resultSet.getString("end_date"),
                    resultSet.getDouble("start_price"),
                    resultSet.getDouble("min_increment"),
                    resultSet.getInt("warranty_months")
            );
        } else if (TYPE_VEHICLE.equals(type)) {
            item = new Vehicle(
                    resultSet.getString("description"),
                    resultSet.getString("name"),
                    resultSet.getString("start_date"),
                    resultSet.getString("end_date"),
                    resultSet.getDouble("start_price"),
                    resultSet.getDouble("min_increment"),
                    resultSet.getString("brand")
            );
        } else {
            item = new Art(
                    resultSet.getString("description"),
                    resultSet.getString("name"),
                    resultSet.getString("start_date"),
                    resultSet.getString("end_date"),
                    resultSet.getDouble("start_price"),
                    resultSet.getDouble("min_increment"),
                    resultSet.getString("author")
            );
        }

        item.setId(resultSet.getString("id"));
        item.setHighestCurrentPrice(resultSet.getDouble("highest_current_price"));
        item.setSellerId(resultSet.getString("seller_id"));
        return item;
    }

    private String resolveType(Item item) {
        if (item instanceof Electronics) {
            return TYPE_ELECTRONICS;
        }
        if (item instanceof Vehicle) {
            return TYPE_VEHICLE;
        }
        return TYPE_ART;
    }
}
