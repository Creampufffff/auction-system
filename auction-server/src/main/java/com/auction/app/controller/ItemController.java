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
            return new ApiResponseDTO(true, "Tạo sản phẩm thành công. ID: " + item.getId());
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Lỗi tạo sản phẩm: " + e.getMessage());
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
            return new ApiResponseDTO(true, "Xóa sản phẩm thành công");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Lỗi xóa sản phẩm: " + e.getMessage());
        }
    }
}

