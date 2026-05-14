package com.auction.app.controller;

import com.app.common.dto.ApiResponseDTO;
import com.app.common.entity.Item;
import com.auction.app.service.ItemService;

import java.util.List;

public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    // ✅ Dùng DTO Response
    public ApiResponseDTO createItem(Item item) {
        try {
            itemService.saveItem(item);
            return new ApiResponseDTO(true, "Item created successfully. ID: " + item.getId());
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Error creating item: " + e.getMessage());
        }
    }

    public Item getItem(String itemId) {
        return itemService.getById(itemId);
    }

    public List<Item> getAllItems() {
        return itemService.getItemsList();
    }

    // ✅ Dùng DTO Response
    public ApiResponseDTO deleteItem(String itemId) {
        try {
            itemService.deleteItem(itemId);
            return new ApiResponseDTO(true, "Item deleted successfully");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Error deleting item: " + e.getMessage());
        }
    }
}

