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



    private static final String ITEM_SELECT_SQL = """
            SELECT i.id,
                   i.seller_id,
                   i.type,
                   i.name,
                   i.description,
                   i.start_date,
                   i.end_date,
                   i.start_price,
                   i.min_increment,
                   i.highest_current_price,
                   i.image_blob,
                   a.author,
                   e.warranty_months,
                   v.brand
            FROM items i
            LEFT JOIN art a ON a.id = i.id
            LEFT JOIN electronics e ON e.id = i.id
            LEFT JOIN vehicle v ON v.id = i.id
            """;

    @Override
    public Item findById(String id) {
        String sql = ITEM_SELECT_SQL + " WHERE i.id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapItem(resultSet);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding item " + id + ": " + e.getMessage());
        }

        return null;
    }

    @Override
    public List<Item> findAll() {
        String sql = ITEM_SELECT_SQL + " ORDER BY i.name";
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
            connection.setAutoCommit(false);

            try {
                boolean saved = save(entity, connection);
                connection.commit();
                return saved;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save item: " + entity.getId(), e);
        }
    }

    boolean save(Item entity, Connection connection) throws SQLException {
        String sql = """
                INSERT INTO items (
                    id, seller_id, type, name, description, start_date, end_date,
                    start_price, min_increment, highest_current_price, image_blob
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    seller_id = VALUES(seller_id),
                    type = VALUES(type),
                    name = VALUES(name),
                    description = VALUES(description),
                    start_date = VALUES(start_date),
                    end_date = VALUES(end_date),
                    start_price = VALUES(start_price),
                    min_increment = VALUES(min_increment),
                    highest_current_price = VALUES(highest_current_price),
                    image_blob = VALUES(image_blob)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entity.getId());
            statement.setString(2, entity.getSellerId());
            statement.setString(3, resolveType(entity));
            statement.setString(4, entity.getName());
            statement.setString(5, entity.getDescription());
            statement.setString(6, entity.getStartDateString());
            statement.setString(7, entity.getEndDateString());
            statement.setDouble(8, entity.getStartPrice());
            statement.setDouble(9, entity.getMinIncreasement());
            statement.setDouble(10, entity.getHighestCurrentPrice());
            statement.setBytes(11, entity.getImageBlob());
            statement.executeUpdate();
        }

        syncSubtypeTables(entity, connection);
        return true;
    }

    private void syncSubtypeTables(Item entity, Connection connection) throws SQLException {
        try (PreparedStatement deleteArt = connection.prepareStatement("DELETE FROM art WHERE id = ?");
             PreparedStatement deleteElectronics = connection.prepareStatement("DELETE FROM electronics WHERE id = ?");
             PreparedStatement deleteVehicle = connection.prepareStatement("DELETE FROM vehicle WHERE id = ?")) {
            deleteArt.setString(1, entity.getId());
            deleteElectronics.setString(1, entity.getId());
            deleteVehicle.setString(1, entity.getId());
            deleteArt.executeUpdate();
            deleteElectronics.executeUpdate();
            deleteVehicle.executeUpdate();
        }

        if (entity instanceof Art) {
            Art art = (Art) entity;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO art (id, author) VALUES (?, ?) ON DUPLICATE KEY UPDATE author = VALUES(author)")) {
                statement.setString(1, art.getId());
                statement.setString(2, art.getAuthor());
                statement.executeUpdate();
            }
            return;
        }

        if (entity instanceof Electronics) {
            Electronics electronics = (Electronics) entity;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO electronics (id, warranty_months) VALUES (?, ?) ON DUPLICATE KEY UPDATE warranty_months = VALUES(warranty_months)")) {
                statement.setString(1, electronics.getId());
                statement.setInt(2, electronics.getWarrantyMonths());
                statement.executeUpdate();
            }
            return;
        }

        if (entity instanceof Vehicle) {
            Vehicle vehicle = (Vehicle) entity;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO vehicle (id, brand) VALUES (?, ?) ON DUPLICATE KEY UPDATE brand = VALUES(brand)")) {
                statement.setString(1, vehicle.getId());
                statement.setString(2, vehicle.getBrand());
                statement.executeUpdate();
            }
            return;
        }

        throw new IllegalArgumentException("Unsupported item type: " + entity.getClass().getSimpleName());
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
            int warrantyMonths = resultSet.getInt("warranty_months");
            if (resultSet.wasNull()) {
                throw new IllegalStateException("Missing electronics details for item: " + resultSet.getString("id"));
            }
            item = new Electronics(
                    resultSet.getString("description"),
                    resultSet.getString("name"),
                    resultSet.getString("start_date"),
                    resultSet.getString("end_date"),
                    resultSet.getDouble("start_price"),
                    resultSet.getDouble("min_increment"),
                    warrantyMonths
            );
        } else if (TYPE_VEHICLE.equals(type)) {
            String brand = resultSet.getString("brand");
            if (brand == null || brand.isBlank()) {
                throw new IllegalStateException("Missing vehicle details for item: " + resultSet.getString("id"));
            }
            item = new Vehicle(
                    resultSet.getString("description"),
                    resultSet.getString("name"),
                    resultSet.getString("start_date"),
                    resultSet.getString("end_date"),
                    resultSet.getDouble("start_price"),
                    resultSet.getDouble("min_increment"),
                    brand
            );
        } else {
            String author = resultSet.getString("author");
            if (author == null || author.isBlank()) {
                throw new IllegalStateException("Missing art details for item: " + resultSet.getString("id"));
            }
            item = new Art(
                    resultSet.getString("description"),
                    resultSet.getString("name"),
                    resultSet.getString("start_date"),
                    resultSet.getString("end_date"),
                    resultSet.getDouble("start_price"),
                    resultSet.getDouble("min_increment"),
                    author
            );
        }

        item.setId(resultSet.getString("id"));
        item.setHighestCurrentPrice(resultSet.getDouble("highest_current_price"));
        item.setSellerId(resultSet.getString("seller_id"));
        item.setImageBlob(resultSet.getBytes("image_blob"));

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
