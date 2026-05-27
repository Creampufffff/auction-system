package com.auction.app.repository.impl;

import com.app.common.entity.ItemImage;
import com.auction.app.config.DatabaseConfig;
import com.auction.app.repository.ItemImageDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemImageDAOImpl implements ItemImageDAO {

    private static final String TABLE_NAME = "item_images";
    private static final String[] COLUMN_NAMES = {"id", "item_id", "image_path", "image_data", "display_order"};

    private static final String INSERT_SQL = "INSERT INTO " + TABLE_NAME + " (id, item_id, image_path, image_data, display_order) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM " + TABLE_NAME + " WHERE id = ?";
    private static final String SELECT_BY_ITEM_ID_SQL = "SELECT * FROM " + TABLE_NAME + " WHERE item_id = ? ORDER BY display_order ASC";
    private static final String SELECT_THUMBNAIL_SQL = "SELECT * FROM " + TABLE_NAME + " WHERE item_id = ? ORDER BY display_order ASC LIMIT 1";
    private static final String UPDATE_SQL = "UPDATE " + TABLE_NAME + " SET image_path = ?, image_data = ?, display_order = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
    private static final String DELETE_BY_ITEM_SQL = "DELETE FROM " + TABLE_NAME + " WHERE item_id = ?";
    private static final String UPDATE_ORDER_SQL = "UPDATE " + TABLE_NAME + " SET display_order = ? WHERE id = ?";

    private Connection getConnection() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    @Override
    public ItemImage save(ItemImage entity) throws SQLException {
        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(UUID.randomUUID().toString());
        }

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, entity.getId());
            statement.setString(2, entity.getItemId());
            statement.setString(3, entity.getImagePath());
            statement.setBytes(4, entity.getImageData());
            statement.setInt(5, entity.getDisplayOrder());

            statement.executeUpdate();
            return entity;
        }
    }

    @Override
    public ItemImage findById(String id) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapItemImage(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public List<ItemImage> getImagesByItemId(String itemId) throws SQLException {
        List<ItemImage> images = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ITEM_ID_SQL)) {
            statement.setString(1, itemId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    images.add(mapItemImage(resultSet));
                }
            }
        }
        return images;
    }

    @Override
    public ItemImage getThumbnailByItemId(String itemId) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_THUMBNAIL_SQL)) {
            statement.setString(1, itemId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapItemImage(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public void deleteImagesByItemId(String itemId) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_BY_ITEM_SQL)) {
            statement.setString(1, itemId);
            statement.executeUpdate();
        }
    }

    @Override
    public void updateDisplayOrder(String imageId, int order) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_ORDER_SQL)) {
            statement.setInt(1, order);
            statement.setString(2, imageId);
            statement.executeUpdate();
        }
    }

    @Override
    public void update(ItemImage entity) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, entity.getImagePath());
            statement.setBytes(2, entity.getImageData());
            statement.setInt(3, entity.getDisplayOrder());
            statement.setString(4, entity.getId());

            statement.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setString(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<ItemImage> findAll() throws SQLException {
        throw new UnsupportedOperationException("Use getImagesByItemId instead");
    }

    private ItemImage mapItemImage(ResultSet resultSet) throws SQLException {
        ItemImage image = new ItemImage();
        image.setId(resultSet.getString("id"));
        image.setItemId(resultSet.getString("item_id"));
        image.setImagePath(resultSet.getString("image_path"));
        image.setImageData(resultSet.getBytes("image_data"));
        image.setDisplayOrder(resultSet.getInt("display_order"));
        return image;
    }
}

