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
        itemDAO.delete(id);
    }

    @Override
    public Item getById(String id) {
        return itemDAO.findById(id);
    }

    @Override
    public List<Item> getItemsList() {
        return itemDAO.findAll();
    }

    @Override
    public void saveItem(Item item) {
        itemDAO.save(item);
    }
}
