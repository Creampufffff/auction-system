package com.auction.app.controller;

import com.app.common.entity.Item;
import com.auction.app.service.ItemService;

import java.util.List;

public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    public void createItem(Item item) {
        itemService.saveItem(item);
    }

    public Item getItem(String itemId) {
        return itemService.getById(itemId);
    }

    public List<Item> getAllItems() {
        return itemService.getItemsList();
    }

    public void deleteItem(String itemId) {
        itemService.deleteItem(itemId);
    }
}

