package com.auction.app.service.impl;

import com.app.common.entity.Item;
import com.auction.app.repository.ItemDAO;
import com.auction.app.service.ItemService;

import java.util.List;

public class ItemServiceImpl implements ItemService {

    private final ItemDAO itemDAO;

    public ItemServiceImpl(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    @Override
    public void deleteItem(String id) {
        validateId(id);
        if (!itemDAO.delete(id)) {
            throw new IllegalArgumentException("Item not found with id: " + id);
        }
    }

    @Override
    public Item getById(String id) {
        validateId(id);
        return itemDAO.findById(id);
    }

    @Override
    public List<Item> getItemsList() {
        return itemDAO.findAll();
    }

    @Override
    public void saveItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }

        if (item.getName() == null || item.getName().isBlank()) {
            throw new IllegalArgumentException("Item name cannot be empty");
        }

        if (item.getStartDateString() == null || item.getStartDateString().isBlank()) {
            throw new IllegalArgumentException("Start date cannot be empty");
        }

        if (item.getEndDateString() == null || item.getEndDateString().isBlank()) {
            throw new IllegalArgumentException("End date cannot be empty");
        }

        if (item.getStartPrice() < 0) {
            throw new IllegalArgumentException("Start price cannot be less than 0");
        }

        if (item.getMinIncreasement() <= 0) {
            throw new IllegalArgumentException("Minimum increment must be greater than 0");
        }

        if (!itemDAO.save(item)) {
            throw new IllegalStateException("Failed to save item");
        }
    }

    private void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Item ID cannot be empty");
        }
    }
}
